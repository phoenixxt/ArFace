package com.example.ar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.app.ActivityManager
import android.app.Activity
import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import java.lang.Double.parseDouble
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var arFragment: FaceArFragment? = null

    private var faceRegionsRenderable: ModelRenderable? = null
    private var faceMeshTexture: Texture? = null

    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish()) {
            return
        }

        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.face_fragment) as FaceArFragment?

        createModelRenderable()

        loadFaceMesh()

        setupSceneView()
    }

    private fun setupSceneView() {
        val sceneView = arFragment?.arSceneView ?: throw Exception("Something went wrong")

        sceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST

        sceneView.scene.addOnUpdateListener {
            if (faceRegionsRenderable == null || faceMeshTexture == null) {
                return@addOnUpdateListener
            }

            val faceList = sceneView.session?.getAllTrackables(AugmentedFace::class.java) ?: emptyList()

            for (face in faceList) {
                if (!faceNodeMap.containsKey(face)) {
                    val faceNode = AugmentedFaceNode(face)
                    faceNode.setParent(sceneView.scene)
                    faceNode.faceRegionsRenderable = faceRegionsRenderable
                    faceNode.faceMeshTexture = faceMeshTexture
                    faceNodeMap[face] = faceNode
                }
            }

            val iterator = faceNodeMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val face = entry.key
                if (face.trackingState === TrackingState.STOPPED) {
                    val faceNode = entry.value
                    faceNode.setParent(null)
                    iterator.remove()
                }
            }
        }
    }

    private fun loadFaceMesh() {
        Texture.builder()
            .setSource(this, R.drawable.fox_face_mesh_texture)
            .build()
            .thenAccept { texture -> faceMeshTexture = texture }
    }

    private fun createModelRenderable() {
        ModelRenderable.builder()
            .setSource(this, R.raw.fox_face)
            .build()
            .thenAccept { modelRenderable ->
                faceRegionsRenderable = modelRenderable
                modelRenderable.isShadowCaster = false
                modelRenderable.isShadowReceiver = false
            }
    }

    private fun checkIsSupportedDeviceOrFinish(): Boolean {
        if (ArCoreApk.getInstance().checkAvailability(this) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            finish()
            return false
        }

        val openGlVersionString = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion

        if (parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            finish()
            return false
        }
        return true
    }

    companion object {
        private const val MIN_OPENGL_VERSION = 3.0
    }
}
