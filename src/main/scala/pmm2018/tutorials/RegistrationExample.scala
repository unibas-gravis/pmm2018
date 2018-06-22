package pmm2018.tutorials

import java.awt.Color
import java.io.File

import breeze.linalg.DenseVector
import scalismo.common.{RealSpace, VectorField}
import scalismo.geometry.{Point, Vector, _3D}
import scalismo.image.DifferentiableScalarImage
import scalismo.io.MeshIO
import scalismo.kernels.{DiagonalKernel, GaussianKernel}
import scalismo.numerics.{GradientDescentOptimizer, RandomMeshSampler3D}
import scalismo.registration._
import scalismo.statisticalmodel.{GaussianProcess, LowRankGaussianProcess, StatisticalMeshModel}
import scalismo.ui.api.{ScalismoUI, ScalismoUIHeadless}
import scalismo.utils.Random

/**
  * A (free) translation of the code from the registration tutorial to Scalismo 0.16
  */
object RegistrationExample extends App {

  // Since Scalismo 0.16, we are forced to initialize (and seed) the random number generator explicitly
  // at the beginning of the program, in order to get reproducible results.
  implicit val rng = Random(42)

  // We create a ui object.
  val ui = ScalismoUI()  // or use ScalismoUIHeadless to suppress visual output

  // Loading the data (we are using lowres version to safe some computation time)
  val referenceMesh = MeshIO.readMesh(new File("datasets/lowResPaola.stl")).get
  val targetMesh = MeshIO.readMesh(new File("datasets/lowRes323.stl")).get


  // We create a new group in the ui and visualize the target Mesh
  val targetGroup = ui.createGroup("target")
  val targetMeshView = ui.show(targetGroup, targetMesh, "face")

  // by interacting with the view object, we can change color, opacity, ...
  targetMeshView.color = Color.RED


  // all mesh operations can now be conveniently accessed using mesh.operations
  val distImagePaola = referenceMesh.operations.toDistanceImage
  val distImageTarget = targetMesh.operations.toDistanceImage


  // We create a GP and the correpsonding transformation space as in the tutorial
  val gausMixKernel = GaussianKernel[_3D](50f) * 100f + GaussianKernel[_3D](20f) * 50f + GaussianKernel[_3D](10f) * 20f
  val matrixValuedKernel = DiagonalKernel(gausMixKernel, 3)
  val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))
  val gp = GaussianProcess (zeroMean,matrixValuedKernel)
  val sampler = RandomMeshSampler3D(referenceMesh, 300, 42)
  val lowrankGP= LowRankGaussianProcess.approximateGP(gp, sampler, numBasisFunctions = 50)

  val gpTransSpace = GaussianProcessTransformationSpace(lowrankGP)

  // We visualize the model. The easiest way to visualize it is to generate a shape model and add it to the ui
  val model : StatisticalMeshModel = StatisticalMeshModel(referenceMesh, lowrankGP)
  val modelGroup = ui.createGroup("model")
  val ssmView = ui.show(modelGroup, model, "model")

  // the metric now takes already the images it needs to compare and the transformation space
  val metricSampler = RandomMeshSampler3D(referenceMesh, 1000, 42)
  val imageMetric = MeanSquaresMetric(distImagePaola, distImageTarget, gpTransSpace, metricSampler )

  // and so does the regularizer
  val regularizer = L2Regularizer(gpTransSpace)


  // assembling the registration method is greatly simplified compared to the tutorial version
  val registration = Registration(
    metric = imageMetric,
    regularizer = regularizer,
    regularizationWeight = 0.1,
    optimizer = GradientDescentOptimizer(100, 0.05))

  // the registration is run by obtaining an iterator and iterating over it
  val initialParameters = DenseVector.zeros[Double](gpTransSpace.parametersDimensionality)
  val registrationIterator = registration.iterator(initialParameters)

  // This iterator cannow be augmented to include, for example, visualization.
  // Here we update the model coefficients in every iteration
  val incrementedIterator = for (it <- registrationIterator) yield {
    ssmView.shapeModelTransformationView.shapeTransformationView.coefficients = it.parameters
  }

  // To run the registration we consume the iterator.
  incrementedIterator.toSeq.last
}
