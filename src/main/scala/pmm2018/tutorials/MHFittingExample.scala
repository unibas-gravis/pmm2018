package pmm2018.tutorials

import java.awt.Color

import breeze.linalg.{DenseMatrix, DenseVector}
import scalismo.geometry._
import scalismo.io.{LandmarkIO, MeshIO, StatismoIO}
import scalismo.registration.{RigidTransformation, RotationTransform, TranslationTransform}
import scalismo.sampling._
import scalismo.sampling.algorithms.MetropolisHastings
import scalismo.sampling.evaluators.ProductEvaluator
import scalismo.sampling.loggers.AcceptRejectLogger
import scalismo.sampling.proposals.MixtureProposal
import scalismo.statisticalmodel.{MultivariateNormalDistribution, StatisticalMeshModel}
import scalismo.ui.api.ScalismoUI
import scalismo.utils.{Memoize, Random}

//----------------------
// Simple parameter class
//----------------------
case class Parameters(rotationCenter : Point[_3D],
                      rotationParameters : DenseVector[Double],
                      translationParameters : DenseVector[Double],
                      modelCoefficients : DenseVector[Double]) {

  def rigidTransform : RigidTransformation[_3D] = {
    val rotationTransform = RotationTransform(rotationParameters(0), rotationParameters(1), rotationParameters(2), rotationCenter)
    val translationTransform = TranslationTransform(Vector3D(translationParameters(0), translationParameters(1), translationParameters(2)))
    RigidTransformation(translationTransform, rotationTransform)
  }
}




//---------------------------------
// Proposals
//---------------------------------
case class ShapeUpdateProposal(paramVectorSize : Int, stdev: Double)(implicit rng : Random) extends ProposalGenerator[Parameters] with TransitionProbability[Parameters] {

  val perturbationDistr = new MultivariateNormalDistribution( DenseVector.zeros(paramVectorSize),
    DenseMatrix.eye[Double](paramVectorSize) * stdev)


  override def propose(theta: Parameters): Parameters = {
    val perturbation = perturbationDistr.sample()
    val thetaPrime = theta.copy(modelCoefficients = theta.modelCoefficients + perturbationDistr.sample)
    thetaPrime
  }

  override def logTransitionProbability(from: Parameters, to: Parameters) = {
    val residual = to.modelCoefficients - from.modelCoefficients
    perturbationDistr.logpdf(residual)
  }

}

case class RotationUpdateProposal(stddev: Double)(implicit rng : Random) extends
  ProposalGenerator[Parameters]  with TransitionProbability[Parameters] {

  val perturbationDistr = new MultivariateNormalDistribution( DenseVector.zeros(3),
    DenseMatrix.eye[Double](3) * stddev)

  def propose(theta: Parameters): Parameters = {
    theta.copy(rotationParameters = theta.rotationParameters +  perturbationDistr.sample)
  }

  override def logTransitionProbability(from: Parameters, to: Parameters) = {
    val residual = to.rotationParameters - from.rotationParameters
    perturbationDistr.logpdf(residual)
  }
}

case class TranslationUpdateProposal(stddev: Double)(implicit rng : Random) extends
  ProposalGenerator[Parameters]  with TransitionProbability[Parameters] {

  val perturbationDistr = new MultivariateNormalDistribution( DenseVector.zeros(3),
    DenseMatrix.eye[Double](3) * stddev)

  def propose(theta: Parameters): Parameters= {
    theta.copy(translationParameters = theta.translationParameters + perturbationDistr.sample())
  }

  override def logTransitionProbability(from: Parameters, to: Parameters) = {
    val residual = to.translationParameters - from.translationParameters
    perturbationDistr.logpdf(residual)
  }
}


//---------------------------------
// Evaluators
//---------------------------------
case class ShapePriorEvaluator(model : StatisticalMeshModel) extends DistributionEvaluator[Parameters] {

  override def logValue(theta: Parameters): Double = {
    model.gp.logpdf(theta.modelCoefficients)
  }
}


case class ProximityEvaluator(model: StatisticalMeshModel, targetLandmarks : Seq[Point[_3D]],    sdev: Double = 1.0) extends DistributionEvaluator[Parameters] {

  val  uncertainty = MultivariateNormalDistribution(DenseVector(0.0, 0.0, 0.0), DenseMatrix.eye[Double](3) * (sdev * sdev))

  private def computeLogValue(theta : Parameters) = {

    val currModelInstance = model.instance(theta.modelCoefficients).transform(theta.rigidTransform)

    val likelihoods = targetLandmarks.map{ targetLandmark =>

      val closestPointCurrentFit = currModelInstance.pointSet.findClosestPoint(targetLandmark).point
      val observedDeformation =  targetLandmark - closestPointCurrentFit
      uncertainty.logpdf(observedDeformation.toBreezeVector)
    }

    val loglikelihood = likelihoods.sum
    loglikelihood
  }
  // we cache the logValue computation, cause it is expensive
  val cachedLogValue = Memoize(computeLogValue, 5)
  override def logValue(theta: Parameters): Double = {
    cachedLogValue(theta)
  }
}

//---------------------------------
// Main program
//---------------------------------
object MHFittingExample {

  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(42L)


    val ui = ScalismoUI()
    val modelGroup = ui.createGroup("model")
    val targetGroup = ui.createGroup("target")


    val model = StatismoIO.readStatismoMeshModel(new java.io.File("datasets/bfm-10faces.h5")).get
    val target = MeshIO.readMesh(new java.io.File("datasets/target.stl")).get

    val modelView = ui.show(modelGroup, model, "model")

    val targetLms = LandmarkIO.readLandmarksJson[_3D](new java.io.File("datasets/targetLM_mcmc.json")).get
    val targetLmViews = ui.show(targetGroup, targetLms, "target")
    targetLmViews.map(v => v.color = Color.RED)

    val targetPoints = targetLms.map(l => l.point)

    val shapeUpdateProposal = ShapeUpdateProposal(model.rank, 0.1f)
    val rotationUpdateProposal = RotationUpdateProposal(0.01f)
    val translationUpdateProposal = TranslationUpdateProposal(1.0f)
    val poseAndShapeGenerator = MixtureProposal.fromProposalsWithTransition((0.6, shapeUpdateProposal),(0.2, rotationUpdateProposal), (0.2, translationUpdateProposal))

    val likelihoodEvaluator = ProximityEvaluator(model, targetPoints, 3.0)
    val priorEvaluator = ShapePriorEvaluator(model)

    val posteriorEvaluator = ProductEvaluator(priorEvaluator, likelihoodEvaluator)



    val logger = new AcceptRejectLogger[Parameters] {

      override def accept(current: Parameters, sample: Parameters, generator: ProposalGenerator[Parameters], evaluator: DistributionEvaluator[Parameters]): Unit = {
        println(s"Accepted proposal generated by $generator (probability ${evaluator.logValue(sample)})")
      }

      override def reject(current: Parameters, sample: Parameters, generator: ProposalGenerator[Parameters], evaluator: DistributionEvaluator[Parameters]): Unit = {
        println(s"Rejected proposal generated by $generator (probability ${evaluator.logValue(sample)})")
      }
    }


    val chain = MetropolisHastings(poseAndShapeGenerator, posteriorEvaluator)

    val rotationCenter = model.mean.pointSet.points.foldLeft(Point3D(0,0,0))((sum, p) => sum + (p.toVector / model.mean.pointSet.numberOfPoints))
    val initialParameters = Parameters(rotationCenter, DenseVector.zeros[Double](3), DenseVector.zeros[Double](3), DenseVector.zeros[Double](model.rank))

    val mhIterator = chain.iterator(initialParameters, logger)

    //
    val samplingIterator = for (theta <- mhIterator) yield {

      // we update the pose transformation in the gui
      modelView.shapeModelTransformationView.poseTransformationView.transformation = theta.rigidTransform

      // and the shape transformation (model coefficients)
      modelView.shapeModelTransformationView.shapeTransformationView.coefficients = theta.modelCoefficients
      theta
    }

    val samples = samplingIterator.drop(100).take(50).toSeq
    val bestSample = samples.maxBy(posteriorEvaluator.logValue)

  }


}
