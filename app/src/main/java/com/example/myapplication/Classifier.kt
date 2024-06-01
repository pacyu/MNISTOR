package com.example.myapplication

import android.content.res.AssetManager
import android.os.Trace
import android.util.Log
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

class Classifier {
    private val TAG = "Classifier"
    private val modelFilename: String = "mnist_model_graph.pb"
    private var inferenceInterface: TensorFlowInferenceInterface? = null
    private val inputName: String="input"
    private val outputName: String="output"
    private var output: FloatArray? = null

    fun create(assetManager: AssetManager) {
        try {
//            System.loadLibrary("tensorflow_inference")
            inferenceInterface = TensorFlowInferenceInterface(assetManager, modelFilename)
        } catch (e: Exception) {
            Log.e("ERROR", e.message.toString())
        }

        inferenceInterface!!.graph()
    }

    fun recognizing(input: List<Float>, n: Long): FloatArray? {
        Trace.beginSection("recognizeImage")

        Trace.beginSection("feed")
        inferenceInterface!!.feed(inputName, input.toFloatArray(), n, 28 * 28)
        Trace.endSection()

        Trace.beginSection("run")
        val outputNames = arrayOf(outputName)
        inferenceInterface!!.run(outputNames)
        Trace.endSection()

        Trace.beginSection("fetch")
        output = FloatArray((n*10).toInt())
        inferenceInterface!!.fetch(outputName, output)
        Trace.endSection()

        Trace.endSection()
        return output
    }

}