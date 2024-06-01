package com.example.myapplication

import android.content.res.AssetManager
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Core.BORDER_CONSTANT
import org.opencv.core.Core.REDUCE_SUM
import org.opencv.core.Core.copyMakeBorder
import org.opencv.core.Core.reduce
import org.opencv.core.CvType.CV_32FC1
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.at
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgcodecs.Imgcodecs.imread
import org.opencv.imgcodecs.Imgcodecs.imwrite
import org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE
import org.opencv.imgproc.Imgproc.FONT_HERSHEY_COMPLEX
import org.opencv.imgproc.Imgproc.RETR_EXTERNAL
import org.opencv.imgproc.Imgproc.THRESH_TOZERO
import org.opencv.imgproc.Imgproc.boundingRect
import org.opencv.imgproc.Imgproc.dilate
import org.opencv.imgproc.Imgproc.findContours
import org.opencv.imgproc.Imgproc.putText
import org.opencv.imgproc.Imgproc.rectangle
import org.opencv.imgproc.Imgproc.resize
import org.opencv.imgproc.Imgproc.threshold
import org.opencv.utils.Converters
import java.lang.Math.abs

class DigitsClassifyHelper constructor(private val filepath: String) {
    private var borders: MutableList<Rect>?=null
    private var img: Mat?=null
    private val TAG = "DigitsClassifyHelper"

    private fun accessPixel(img: Mat): Mat {
        val nImage = Mat(img.height(), img.width(), CV_8UC1, Scalar.all(255.0))

        Core.subtract(nImage, img, nImage)
        return nImage
    }

    private fun accessBinary(img: Mat, thr: Double=128.0): Mat {
        val image = accessPixel(img)

        val kernel = Mat.ones(Size(3.0,3.0), CV_8UC1)
        val dst = Mat()
        dilate(image, dst, kernel)
        val outImage = Mat()
        threshold(dst, outImage, thr, .0, THRESH_TOZERO)

        return outImage
    }

    private fun extractPeek(vec: Mat, minVal:Double=10.0, minRect:Double=20.0):
            MutableList<Pair<Int, Int>> {
        val extrackPoints: MutableList<Pair<Int, Int>> = ArrayList()
        var startPoint: Int=-1
        var endPoint: Int=-1

        val width = vec.width()
        for(i in 0 until width) {
            val e = vec.at<Int>(0, i).v
            if(e > minVal && startPoint == -1) startPoint = i
            else if(e < minVal && startPoint != -1) endPoint = i

            if(startPoint!=-1 && endPoint!=-1) {
                extrackPoints.add(Pair(startPoint, endPoint))
                startPoint = -1
                endPoint = -1
            }
        }

        for(p in extrackPoints) {
            if(p.second - p.first < minRect)
                extrackPoints.remove(p)
        }
        Log.e(TAG, "extractPeek $extrackPoints")

        return extrackPoints
    }

    private fun findBorderHistogram(img: Mat): MutableList<Pair<Point, Point>> {
        val borders: MutableList<Pair<Point, Point>> = ArrayList()
        val aimg = accessBinary(img)
        val horiVals = Mat()
        reduce(aimg, horiVals, 1, REDUCE_SUM)
        val horiPoints = extractPeek(horiVals)

        for(p in horiPoints) {
            val extractImg = aimg.colRange(p.first, p.second)
            val vecVals = Mat()
            reduce(extractImg, vecVals, 0, REDUCE_SUM)
            val vecPoints = extractPeek(vecVals,0.0)
            for(vecp in vecPoints) {
                borders.add(Pair(
                    Point(vecp.first.toDouble(), p.first.toDouble()),
                    Point(vecp.second.toDouble(), p.second.toDouble())))
            }
        }
        return borders
    }

    private fun findBorderContours(img: Mat, maxArea: Int=50): MutableList<Rect> {
        val borders: MutableList<Rect> = arrayListOf()
        val imgBinary = accessBinary(img)
        val contours: MutableList<MatOfPoint> = arrayListOf()
        val hierarchy = Mat()

        findContours(imgBinary, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE)

        for(contour in contours) {
            val rect = boundingRect(contour)
            if(rect.width*rect.height > maxArea) {
                borders.add(rect)
            }
        }
        return borders
    }

    private fun imageLabeling(img: Mat,
                            borders: MutableList<Rect>,
                            result: ArrayList<Int>): Mat {
        Log.i(TAG, "=======================>imageLabeling")
        borders.forEachIndexed { index, rect ->
            rectangle(img,
                Point(rect.x.toDouble(), rect.y.toDouble()),
                Point(rect.x+rect.width.toDouble(), rect.y+rect.height.toDouble()),
                Scalar(0.0, 0.0, 255.0))
            if(result.isNotEmpty()) {
                Log.i(TAG, "=======================>result is not empty")
                putText(img,
                    result[index].toString(),
                    Point(rect.x.toDouble(), rect.y.toDouble()),
                    FONT_HERSHEY_COMPLEX, 2.0,
                    Scalar(0.0,255.0,0.0),
                    1)
            }
        }

        Log.e(TAG, "imageLabeling $img")

        return img
    }

    private fun transMnist(img: Mat, borders: MutableList<Rect>, width: Int=28, height: Int=28): List<Float> {
        val images: MutableList<Float> = ArrayList()
        val imageBinary = accessBinary(img)
        borders.forEach { rect ->
            val subImage = Mat(imageBinary, rect)

            val extendPixel = (abs(subImage.width() - subImage.height()) / 2)
            var targetImage = Mat()
            copyMakeBorder(subImage, targetImage,7,7, extendPixel+7,extendPixel+7, BORDER_CONSTANT)
            resize(targetImage, targetImage, Size(width.toDouble(), height.toDouble()))
            targetImage = targetImage.reshape(0, width*height)
            targetImage.convertTo(targetImage, CV_32FC1, 1.0/255.0)
            val nImage: List<Float> = ArrayList()
            Converters.Mat_to_vector_float(targetImage, nImage)
            images += nImage
        }

        return images
    }

    fun toSave(assetManager: AssetManager): Pair<Boolean, String> {
        val classifier = Classifier()
        var labeledImgFile = ""

        classifier.create(assetManager)

        borders = findBorderContours(img!!)
        Log.e(TAG, "Number of borders: ${borders!!.size}")
        if(!borders.isNullOrEmpty()) {
            Log.i(TAG, "=====================>border is not null")
            val images = transMnist(img!!, borders!!)
            val result = classifier.recognizing(images, borders!!.size.toLong())
            Log.e(TAG, "result: ${result!!.size}")

            val recognizedNumbers = ArrayList<Int>()
            for (i in 0 until result.size step 10) {
                val probs = result.slice(i..i+9)
                recognizedNumbers.add(probs.indexOf(probs.max()))
            }

            Log.i(TAG, "=====================>recognizing successful")
            val originImage = imread(filepath)
            val imgLabeled = imageLabeling(originImage, borders!!, recognizedNumbers)
            labeledImgFile = filepath.replace(".jpg", "_labeled.jpg")
            imwrite(labeledImgFile, imgLabeled)

            return Pair(true, labeledImgFile)
        } else {
            labeledImgFile = "识别失败！"
            return Pair(false, labeledImgFile)
        }
    }

    fun init() {
        if(OpenCVLoader.initDebug()) {
            img = imread(filepath)
            resize(img, img,
                Size(600.0, 800.0))
            imwrite(filepath, img)
            img = imread(filepath, Imgcodecs.IMREAD_GRAYSCALE)
        }
        else {
            Log.e("DigitsClassify", "Opencv initialize failed!")
        }
    }

}
