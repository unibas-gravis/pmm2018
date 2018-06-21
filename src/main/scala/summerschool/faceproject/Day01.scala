package summerschool.faceproject

/*
 * Copyright University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
object Day01 extends App {

  import java.io.File

  import breeze.numerics.abs
  import scalismo.faces.color.{RGB, RGBA}
  import scalismo.faces.gui.GUIBlock._
  import scalismo.faces.gui.ImagePanel
  import scalismo.faces.image.PixelImage
  import scalismo.faces.io.{MoMoIO, PixelImageIO, RenderParameterIO}
  import scalismo.faces.parameters._
  import scalismo.faces.sampling.face.MoMoRenderer
  import scalismo.geometry.{Vector, _3D}


  // load image and render first image
  scalismo.initialize()

  val modelFile = new File("data/model2009-bfm.h5")
  val model = MoMoIO.read(modelFile).get

  val modelInstance = MoMoInstance.zero(
    shapeComponents = 5,
    colorComponents = 5,
    expressionComponents = 0,
    modelFile.toURI)
  val parameters = RenderParameter.default.
    withMoMo(modelInstance).
    forImageSize(640, 480)

  val renderer = MoMoRenderer(model)

  val image = renderer.renderImage(parameters)
  val panel = ImagePanel(image,RGBA.Black.toAWTColor)
  panel.displayIn("First Image")


  // modify parameters and render another image
  val scaledParameters = parameters.copy(pose = parameters.pose.copy(scaling = 1.3))
  val image2 = renderer.renderImage(scaledParameters)
  val panel2 = ImagePanel(image2)
  panel2.displayIn("Modified Image")


  val newShape = IndexedSeq(3.0, 3.0) ++ modelInstance.shape.drop(2)
  val changedInstance = modelInstance.copy(shape = newShape)
  val changedShape = scaledParameters.copy(momo = changedInstance)
  val image3 = renderer.renderImage(changedShape)
  panel2.updateImage(image3)


  // calculate difference between images
  def differenceImage(
                       imageA: PixelImage[RGBA],
                       imageB: PixelImage[RGBA]
                     ) : PixelImage[RGBA] = {
    imageA.zip(imageB).map { case (pixel1, pixel2) =>
      val colorDifference = pixel1 - pixel2
      val scaledTo_0_1 = colorDifference * 0.5 + RGBA(0.5, 0.5, 0.5, 1.0)
      scaledTo_0_1
    }
  }

  val difference = differenceImage(image,image3)


  // setup UI for i1, i2 and difference image
  val targetPanel = ImagePanel(image)
  val imagePanel = ImagePanel(image3)
  val differencePanel = ImagePanel(difference)

  val sideBySide = shelf(targetPanel, imagePanel)
  val overview = stack(
    sideBySide,
    differencePanel)
  overview.displayIn("Comparison")


  def meanSquaredDifference(imageA: PixelImage[RGBA], imageB: PixelImage[RGBA]) = {
    val diffsSquared = {
      for (
        (colA, colB) <- imageA.values.zip(imageB.values)
        if ( colB.a>0 && colA.a>0 )
      ) yield {
        val diff = (colA - colB)
        diff.dot(diff)
      }
    }.toSeq
    diffsSquared.sum / diffsSquared.size
  }

  {
    println(meanSquaredDifference(image, image3))

    val targetParameters = RenderParameterIO.read(new File("data/day1/targetParameters.rps")).get
    val targetImage = renderer.renderImage(targetParameters)
    targetPanel.updateImage(targetImage)

    // try parameters
    val states = for (angle <- -3.145 to 3.145 by 0.1) yield {
      val rotatedParams = parameters.copy(pose = parameters.pose.copy(roll = angle))

      val rotatedImage = renderer.renderImage(rotatedParams)
      imagePanel.updateImage(rotatedImage)

      val diff = differenceImage(targetImage, rotatedImage)
      differencePanel.updateImage(diff)

      val value = meanSquaredDifference(targetImage, rotatedImage)
      val diffGroundTruth = abs(targetParameters.pose.roll - angle)

      (angle, value, diffGroundTruth)
    }


    // get best parameter
    val best = states.sortBy(t => t._2).head
    println(s"Best value: ${best._2} at roll-angle: ${best._1/math.Pi*180} with difference to ground truth: ${best._3/math.Pi*180}")

    // visualize best pose
    val bestParams = parameters.copy(pose = parameters.pose.copy(roll = best._1))
    val bestImage = renderer.renderImage(bestParams)
    imagePanel.updateImage(bestImage)
  val bestImageDiff = differenceImage(bestImage,targetImage)
  differencePanel.updateImage(bestImageDiff)

//    // plot values
//    import breeze.plot._
//
//    val f = Figure("Roll dependent image difference")
//    val p = f.subplot(0)
//    val (x, y, d) = states.unzip3
//    p += plot(x, y, name = "MSE of images")
//    p += plot(x, d, name = "abs diff roll [radians]")
//    p.ylim = (0.0,0.3)
//    p.legend = true

  }


  // Challenge
//  {
//    // get the correct pose
//    // dont take too wide angle to test
//    // make 1 level of grid hierarchy
//    // .par for one angle
//
//    val target = PixelImageIO.read[RGBA](new File("data/day1/targetImages/pose.png")).get
//    targetPanel.updateImage(target)
//
//    val i = MoMoInstance.fromCoefficients(model.zeroCoefficients,modelFile.toURI)
//    val momo550 = i.withNumberOfCoefficients(5,5,0)
//    val bluePrint = RenderParameter.defaultSquare.
//      withImageSize(ImageSize(target.width,target.height)).
//      withMoMo(momo550)
//
//    val gridSearch = {
//      for(
//        roll <- (-.5 to .5 by 0.1).par;
//        pitch <- -.5 to .5 by 0.1;
//        yaw <- -.5 to .5 by 0.1
//      ) yield {
//        println(yaw)
//        //      val p = bluePrint.copy(pose = Pose(1.0,Vector(dx,dy,1000+dz),yaw,roll,pitch))
//        val p = bluePrint.copy(pose = Pose(1.0,Vector(0,0,-1000),roll,yaw,pitch))
//        val generated = renderer.renderImage(p)
//        //      imagePanel.updateImage(generated)
//        val diff = meanSquaredDifference(target,generated)
//        (diff,p)
//      }
//    }.toIndexedSeq
//
//    val best = gridSearch.minBy(_._1)
//    val bestImage = renderer.renderImage(best._2)
//    imagePanel.updateImage(bestImage)
//
//
//    import breeze.plot._
//
//    val f = Figure("Roll dependent image difference")
//    val p = f.subplot(0)
//    val x = gridSearch.indices.map(_.toDouble)
//    val y = gridSearch.map(_._1)
//    p += plot(x, y, name = "roll")
//
//    { // level 2 search
//      val gridSearch2 = {
//        for(
//          roll <- (-.1 to .1 by 0.01).map(_+best._2.pose.roll).par;
//          pitch <- (-.1 to .1 by 0.01).map(_+best._2.pose.pitch);
//          yaw <- (-.1 to .1 by 0.01).map(_+best._2.pose.yaw)
//        ) yield {
//          println(yaw)
//          //      val p = bluePrint.copy(pose = Pose(1.0,Vector(dx,dy,1000+dz),yaw,roll,pitch))
//          val p = bluePrint.copy(pose = Pose(1.0,Vector(0,0,-1000),roll,yaw,pitch))
//          val generated = renderer.renderImage(p)
//          //      imagePanel.updateImage(generated)
//          val diff = meanSquaredDifference(target,generated)
//          (diff,p)
//        }
//      }.toIndexedSeq
//
//      val best2 = gridSearch2.minBy(_._1)
//      val bestImage2 = renderer.renderImage(best2._2)
//      imagePanel.updateImage(bestImage2)
//
//
//      import breeze.plot._
//
//      val f = Figure("Roll dependent image difference")
//      val p = f.subplot(0)
//      val x = gridSearch2.indices.map(_.toDouble)
//      val y = gridSearch2.map(_._1)
//      p += plot(x, y, name = "roll")
//    }
//  }

  { // Pose
    val target = PixelImageIO.read[RGBA](new File("data/day1/targetImages/pose.png")).get
    targetPanel.updateImage(target)

    val i = MoMoInstance.fromCoefficients(model.zeroCoefficients,modelFile.toURI)
    val momo550 = i.withNumberOfCoefficients(5,5,0)
    val bluePrint = RenderParameter.defaultSquare.
      withImageSize(ImageSize(target.width,target.height)).
      withMoMo(momo550)

    def poseGrid(mean: RenderParameter, delta: Pose, target: PixelImage[RGBA]): (Double,RenderParameter) = {
      val gridSamples = for (
        roll <- (-3 to 3 by 1).map{d1: Int => d1 * delta.roll + mean.pose.roll};
        pitch <- (-3 to 3 by 1).map{d2: Int => d2 * delta.pitch + mean.pose.pitch}.par;
        yaw <- (-3 to 3 by 1).map{ d3: Int => d3 * delta.yaw + mean.pose.yaw}
      ) yield {
        val newPose = mean.pose.copy(roll = roll, yaw = yaw, pitch = pitch)
        val newParams = mean.copy(pose = newPose)
        val rendered = renderer.renderImage(newParams)
        val value = meanSquaredDifference(target, rendered)
        (value, newParams)
      }
      val best = gridSamples.minBy(_._1)


      if ( delta.yaw < 0.005 ) best
      else {
        val newDelta = delta.copy(
          yaw = delta.yaw*0.3,
          pitch = delta.pitch*0.3,
          roll = delta.roll*0.3
        )
        val bestImage = renderer.renderImage(best._2)
        imagePanel.updateImage(bestImage)
        poseGrid(best._2,newDelta,target)
      }
    }

    val best = poseGrid(bluePrint,Pose(0.0,Vector(0,0,0),0.3,0.3,0.3),target)
    val bestImage = renderer.renderImage(best._2)
    imagePanel.updateImage(bestImage)
  }


  { // Directional light
    val target = PixelImageIO.read[RGBA](new File("data/day1/targetImages/environmentMap.png")).get
    targetPanel.updateImage(target)

    val i = MoMoInstance.fromCoefficients(model.zeroCoefficients,modelFile.toURI)
    val momo550 = i.withNumberOfCoefficients(5,5,0)
    val bluePrint = RenderParameter.defaultSquare.
      withImageSize(ImageSize(target.width,target.height)).
      withMoMo(momo550)

    def poseGrid(bluePrint: RenderParameter, mean: (RGB,Vector[_3D]), delta: (RGB,Vector[_3D]), target: PixelImage[RGBA]): (Double,(RGB,Vector[_3D])) = {
      val gridSamples = for (
        r <- (-1 to 1 by 1).map{d1: Int => d1 * delta._1.r + mean._1.r};
        g <- (-1 to 1 by 1).map{d1: Int => d1 * delta._1.g + mean._1.g};
        b <- (-1 to 1 by 1).map{d1: Int => d1 * delta._1.b + mean._1.b};
        x <- (-1 to 1 by 1).map{d1: Int => d1 * delta._2.x + mean._2.x};
        y <- (-1 to 1 by 1).map{d2: Int => d2 * delta._2.y + mean._2.y}.par;
        z <- (-1 to 1 by 1).map{ d3: Int => d3 * delta._2.z + mean._2.z}
      ) yield {
        val diffuse = RGB(r,g,b)
        val direction = Vector(x,y,z)
        val newEnvMap: SphericalHarmonicsLight = SphericalHarmonicsLight.fromAmbientDiffuse(RGB(0.5,0.5,0.5),diffuse,direction)
        val newParams = bluePrint.copy(environmentMap = newEnvMap)
        val rendered = renderer.renderImage(newParams)
        imagePanel.updateImage(rendered)
        val value = meanSquaredDifference(target, rendered)
        (value, (diffuse,direction))
      }
      val best = gridSamples.minBy(_._1)


      if ( delta._1.r < 0.005 ) best
      else {
        val newDelta = (delta._1*0.5,delta._2*0.5)
        val newEnvMap = SphericalHarmonicsLight.fromAmbientDiffuse(RGB(0.5,0.5,0.5),best._2._1,best._2._2)
        val newParams = bluePrint.copy(environmentMap = newEnvMap)
        val bestImage = renderer.renderImage(newParams)
        imagePanel.updateImage(bestImage)
        poseGrid(bluePrint,best._2,newDelta,target)
      }
    }

    val mean = (RGB(0.5,0.5,0.5),Vector(0.5,0.5,0.5))
    val delta = (RGB(0.4,0.4,0.4),Vector(0.3,0.3,0.3))
    val best = poseGrid(bluePrint,mean,delta,target)
    val newEnvMap = SphericalHarmonicsLight.fromAmbientDiffuse(RGB(0.5,0.5,0.5),best._2._1,best._2._2)
    val newParams = bluePrint.copy(environmentMap = newEnvMap)
    val bestImage = renderer.renderImage(newParams)
    imagePanel.updateImage(bestImage)

  }


//  { // Model
//    val target = PixelImageIO.read[RGBA](new File("data/day1/targetImages/momo.png")).get
//    targetPanel.updateImage(target)
//
//    val i = MoMoInstance.fromCoefficients(model.zeroCoefficients,modelFile.toURI)
//    val momo550 = i.withNumberOfCoefficients(5,5,0)
//    val bluePrint = RenderParameter.defaultSquare.
//      withImageSize(ImageSize(target.width,target.height)).
//      withMoMo(momo550)
//
//    def shapeGrid(bluePrint: RenderParameter, delta: MoMoInstance, target: PixelImage[RGBA]): (Double,MoMoInstance) = {
//      val gridSamples = for (
//        s0 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.shape(0) + bluePrint.momo.shape(0)};
//        s1 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.shape(1) + bluePrint.momo.shape(1)};
//        s2 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.shape(2) + bluePrint.momo.shape(2)};
//        s3 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.shape(3) + bluePrint.momo.shape(3)};
//        s4 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.shape(4) + bluePrint.momo.shape(4)}
//      ) yield {
//        val newInstance = bluePrint.momo.copy(
//          shape = IndexedSeq(s0,s1,s2,s3,s4)
//        )
//        val newParams = bluePrint.copy(momo = newInstance)
//        val rendered = renderer.renderImage(newParams)
//        imagePanel.updateImage(rendered)
//        val value = meanSquaredDifference(target, rendered)
//        (value, newInstance)
//      }
//      val best = gridSamples.minBy(_._1)
//
//
//      if ( delta.shape(0) < 0.01 ) best
//      else {
//        val newDelta = MoMoInstance(
//          delta.shape.map(_*0.7),
//          delta.color,
//          delta.expression,
//          delta.modelURI
//        )
//        val newParams = bluePrint.copy( momo = best._2)
//        val bestImage = renderer.renderImage(newParams)
//        imagePanel.updateImage(bestImage)
//        colorGrid(newParams,newDelta,target)
//      }
//    }
//
//    def colorGrid(bluePrint: RenderParameter, delta: MoMoInstance, target: PixelImage[RGBA]): (Double,MoMoInstance) = {
//      val gridSamples = for (
//        c0 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.color(0) + bluePrint.momo.color(0)};
//        c1 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.color(1) + bluePrint.momo.color(1)};
//        c2 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.color(2) + bluePrint.momo.color(2)};
//        c3 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.color(3) + bluePrint.momo.color(3)};
//        c4 <- (-1 to 1 by 1).map{d1: Int => d1 * delta.color(4) + bluePrint.momo.color(4)}
//      ) yield {
//        val newInstance = bluePrint.momo.copy(
//          color = IndexedSeq(c0,c1,c2,c3,c4)
//        )
//        val newParams = bluePrint.copy(momo = newInstance)
//        val rendered = renderer.renderImage(newParams)
//        imagePanel.updateImage(rendered)
//        val value = meanSquaredDifference(target, rendered)
//        (value, newInstance)
//      }
//      val best = gridSamples.minBy(_._1)
//
//
//      if ( delta.shape(0) < 0.01 ) best
//      else {
//        val newDelta = MoMoInstance(
//          delta.shape,
//          delta.color.map(_*0.5),
//          delta.expression,
//          delta.modelURI
//        )
//        val newParams = bluePrint.copy( momo = best._2)
//        val bestImage = renderer.renderImage(newParams)
//        imagePanel.updateImage(bestImage)
//        shapeGrid(newParams,newDelta,target)
//      }
//    }
//
//    val delta = bluePrint.momo.copy(
//      shape = IndexedSeq.fill(5)(2.0),
//      color = IndexedSeq.fill(5)(2.0)
//    )
//    val best = shapeGrid(bluePrint,delta,target)
//    val newParams = bluePrint.copy(momo = best._2)
//    val rendered = renderer.renderImage(newParams)
//    imagePanel.updateImage(rendered)
//
//  }

  { // grid search
    val N: Int = 1 // number of points to either side of the mean parameter
    val delta: Pose = Pose(0.0,Vector(0,0,0),???,???,???)// parameter change between neighbouring points
    val median: Pose = Pose(1.0,Vector(0,0,0),???,???,???) // median value of parameter

    val evaluationList = for (
      rollStep <- (-N to N by 1);
      yawStep <- (-N to N by 1);
      pitchStep <- (-N to N by 1).par
    ) yield {
      var roll = median.roll + delta.roll * rollStep
      var yaw = median.yaw + delta.yaw * yawStep
      var pitch = median.pitch + delta.pitch * pitchStep


      val pose: Pose = ???
      val param: RenderParameter = ???
      val image: PixelImage[RGBA] = ???
      val value: Double = ???
      (value, param)
    }

    val bestParams = evaluationList.minBy(_._1)
  }

  { // hill climbing
    var bestValue: Double = ???
    var bestParams: RenderParameter = ???

    def randomChange(p: RenderParameter): RenderParameter = ???

    while( ??? ) {
      val newParams: RenderParameter = randomChange(bestParams)
      val newValue: Double = ???

      if ( newValue < bestValue ) {
        bestParams = newParams
        bestValue = newValue
      }
    }
  }

  { // initial parameters
    val modelFile: File = ???
    val targetImage: PixelImage[RGBA] = ???
    val momo550 = MoMoInstance.zero(5,5,0,modelFile.toURI)
    val bluePrint = RenderParameter.defaultSquare.
      withImageSize(ImageSize(targetImage.width,targetImage.height)).
      withMoMo(momo550)

  }

  { // recursive algorithm to be used
    def tryParameters(target: PixelImage[RGBA], median: Pose, delta: Pose) : Pose = {

      val newMedian: Pose = ??? // determine with grid search

      if ( ??? /* stopping criteria */ ) newMedian
      else {
        val newDelta: Pose = ??? // reduce the grid spacing
        tryParameters(target, newMedian, newDelta)
      }
    }
  }

}
