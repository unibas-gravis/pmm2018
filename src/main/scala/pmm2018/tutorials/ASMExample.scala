package pmm2018.tutorials

import java.awt.Color
import java.io.File

import scalismo.common.PointId
import scalismo.geometry.{Landmark, Point3D, _3D}
import scalismo.io.{ActiveShapeModelIO, ImageIO, MeshIO}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.asm.{ActiveShapeModel, PreprocessedImage, ProfileId}
import scalismo.ui.api.ScalismoUI
import scalismo.utils.Random

object ASMExample extends App {

  scalismo.initialize()

  // Since Scalismo 0.16, we are forced to initialize (and seed) the random number generator explicitly
  // at the beginning of the program, in order to get reproducible results.
  implicit val rng = Random(42)

  val asm = ActiveShapeModelIO.readActiveShapeModel(new File("datasets/femurASM/femur-asm.h5")).get

  val ui = ScalismoUI()
  val modelGroup = ui.createGroup("model")
  val targetGroup = ui.createGroup("target")

  ui.show(modelGroup, asm.statisticalModel, "ssm")

  // Scalismo 0.16 does not support visualization of ASM profiles. The following code won't compile
  //ui.show( modelGroup, asm.mean, "mean_intensities")


  val image = ImageIO.read3DScalarImageAsType[Float](new File("datasets/femurASM/14.nii")).get

  // turn the image into a Gaussian smoothed gradient image
  val preProcessedGradientImage = asm.preprocessor(image)

  val pointA = Point3D(108.25238f,158.88396f,839.9854f)
  val pointB = Point3D(103.25238f,158.88396f,839.9854f)

  val lmViews = ui.show(targetGroup, Seq(Landmark("A", pointA), Landmark("B", pointB)), "image")

  val featureVecA = asm.featureExtractor(preProcessedGradientImage, pointA, asm.statisticalModel.mean, PointId(14617)).get
  val featureVecB = asm.featureExtractor(preProcessedGradientImage, pointB, asm.statisticalModel.mean, PointId(14617)).get

  val intensitModel278 = asm.profiles(ProfileId(278)).distribution

  val likelihoodA = intensitModel278.logpdf(featureVecA)
  val likelihoodB = intensitModel278.logpdf(featureVecB)
  println(s"likelihoodA = $likelihoodA ")
  println(s"likelihoodB = $likelihoodB ")

  def likelihoodForMesh(asm : ActiveShapeModel, mesh : TriangleMesh[_3D], preprocessedImage: PreprocessedImage) : Double = {

    val ids = asm.profiles.ids

    val likelihoods = for (id <- ids) yield {
      val profile = asm.profiles(id)
      val profilePointOnMesh = mesh.pointSet.point(profile.pointId)
      val featureAtPoint = asm.featureExtractor(preprocessedImage, profilePointOnMesh, mesh, profile.pointId).get
      profile.distribution.logpdf(featureAtPoint)
    }
    likelihoods.sum
  }

  println("likelihood for mesh: " + likelihoodForMesh(asm, asm.statisticalModel.sample, preProcessedGradientImage))

  val groundTruth = MeshIO.readMesh(new File("datasets/femurASM/14.stl")).get
  val gtGroup = ui.createGroup("groundTruth")

  val gtView = ui.show(gtGroup, groundTruth, "gt")
  gtView.color = Color.RED

  println("likelihood ground truth: " +likelihoodForMesh(asm, groundTruth, preProcessedGradientImage))

}
