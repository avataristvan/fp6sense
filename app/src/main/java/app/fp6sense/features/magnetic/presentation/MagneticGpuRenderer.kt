package app.fp6sense.features.magnetic.presentation

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class MagneticGpuRenderer : GLSurfaceView.Renderer {

    // Shader code
    private val vertexShaderCode = """#version 300 es
        in vec4 vPosition;
        in vec4 vColor;
        in vec3 vNormal;
        
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        
        out vec4 fColor;
        out vec3 fNormal;
        out vec3 fFragPos;

        void main() {
            gl_Position = uMVPMatrix * vPosition;
            fColor = vColor;
            fNormal = mat3(uModelMatrix) * vNormal; // Simplified normal matrix
            fFragPos = vec3(uModelMatrix * vPosition);
        }
    """.trimIndent()

    private val fragmentShaderCode = """#version 300 es
        precision mediump float;
        in vec4 fColor;
        in vec3 fNormal;
        in vec3 fFragPos;
        
        uniform vec4 uColor; // Custom color override
        
        out vec4 fragColor;

        void main() {
            // Simple directional light
            vec3 lightDir = normalize(vec3(0.5, 1.0, 0.8));
            vec3 norm = normalize(fNormal);
            float diff = max(dot(norm, lightDir), 0.2);
            
            vec3 baseColor = uColor.a > 0.0 ? uColor.rgb : fColor.rgb;
            vec3 diffuse = diff * baseColor;
            fragColor = vec4(diffuse, 1.0);
        }
    """.trimIndent()
    
    private var program = 0
    private lateinit var vertexBuffer: FloatBuffer
    
    // Matricies
    private val mMVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mModelMatrix = FloatArray(16)
    
    // Sensor Data to drive visualization
    @Volatile
    var rotationMatrix = FloatArray(9) // From Sensor (Device Rotation)
    @Volatile
    var fieldStrength: Float = 40f // uT
    @Volatile
    var magX: Float = 0f
    @Volatile
    var magY: Float = 0f
    @Volatile
    var magZ: Float = 0f

    // 3D Geometry (Arrow)
    private var vertexCount = 0
    private val COORDS_PER_VERTEX = 3
    private val COLORS_PER_VERTEX = 4
    private val NORMALS_PER_VERTEX = 3
    private val STRIDE = (COORDS_PER_VERTEX + COLORS_PER_VERTEX + NORMALS_PER_VERTEX) * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        // Initialize Rotation Matrix to identity
        // Matrix.setIdentityM(rotationMatrix, 0) - Removed to prevent crash (size 9 vs 16)

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }

        generateArrowMesh()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(program)

        // Camera Position
        Matrix.setLookAtM(mViewMatrix, 0, 
            0f, 0f, 5f,  // Eye
            0f, 0f, 0f,  // Center
            0f, 1f, 0f   // Up
        )

        // --- Arrow 1: North Reference (Gold) ---
        Matrix.setIdentityM(mModelMatrix, 0)

        // Expand 3x3 R to 4x4
        val r4 = FloatArray(16)
        Matrix.setIdentityM(r4, 0)
        if (rotationMatrix.size >= 9) {
            r4[0] = rotationMatrix[0]; r4[1] = rotationMatrix[1]; r4[2] = rotationMatrix[2];
            r4[4] = rotationMatrix[3]; r4[5] = rotationMatrix[4]; r4[6] = rotationMatrix[5];
            r4[8] = rotationMatrix[6]; r4[9] = rotationMatrix[7]; r4[10] = rotationMatrix[8];
        }

        val tempModel = FloatArray(16)
        Matrix.multiplyMM(tempModel, 0, r4, 0, mModelMatrix, 0)
        System.arraycopy(tempModel, 0, mModelMatrix, 0, 16)
        
        // Draw North Arrow (Gold)
        drawMesh(mModelMatrix, 1.0f, 0.84f, 0.0f, 1.0f) // Gold


        // --- Arrow 2: Measured Field (Red) ---
        Matrix.setIdentityM(mModelMatrix, 0)
        
        val strength = Math.sqrt((magX * magX + magY * magY + magZ * magZ).toDouble()).toFloat()
        val scale = (strength / 40f).coerceIn(0.5f, 3.0f)
        Matrix.scaleM(mModelMatrix, 0, scale, scale, scale)

        val magLen = strength
        if (magLen > 0.001f) {
            val mx = magX / magLen
            val my = magY / magLen
            val mz = magZ / magLen
            
            val axisX = mz
            val axisY = 0f
            val axisZ = -mx
            
            val dot = my
            val angleRad = Math.acos(dot.toDouble().coerceIn(-1.0, 1.0))
            val angleDeg = Math.toDegrees(angleRad).toFloat()
            
            Matrix.rotateM(mModelMatrix, 0, angleDeg, axisX, axisY, axisZ)
        }
        
        // Draw Field Arrow (Red)
        drawMesh(mModelMatrix, 1.0f, 0.0f, 0.0f, 1.0f) // Red
    }

    private fun drawMesh(modelMatrix: FloatArray, r: Float, g: Float, b: Float, a: Float) {
        // Calculate MVP
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        // Uniforms
        val mvpHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix")
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mMVPMatrix, 0)
        
        val mHandle = GLES30.glGetUniformLocation(program, "uModelMatrix")
        GLES30.glUniformMatrix4fv(mHandle, 1, false, modelMatrix, 0)
        
        val colorHandle = GLES30.glGetUniformLocation(program, "uColor")
        GLES30.glUniform4f(colorHandle, r, g, b, a)

        // Attributes
        val posHandle = GLES30.glGetAttribLocation(program, "vPosition")
        val colHandle = GLES30.glGetAttribLocation(program, "vColor")
        val normHandle = GLES30.glGetAttribLocation(program, "vNormal")

        // ... rest of binding logic
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, STRIDE, vertexBuffer)
        GLES30.glEnableVertexAttribArray(posHandle)

        vertexBuffer.position(3)
        GLES30.glVertexAttribPointer(colHandle, 4, GLES30.GL_FLOAT, false, STRIDE, vertexBuffer)
        GLES30.glEnableVertexAttribArray(colHandle)
        
        vertexBuffer.position(7)
        GLES30.glVertexAttribPointer(normHandle, 3, GLES30.GL_FLOAT, false, STRIDE, vertexBuffer)
        GLES30.glEnableVertexAttribArray(normHandle)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)

        GLES30.glDisableVertexAttribArray(posHandle)
        GLES30.glDisableVertexAttribArray(colHandle)
        GLES30.glDisableVertexAttribArray(normHandle)
    }

    private fun generateArrowMesh() {
        val vertices = ArrayList<Float>()
        
        // North (Red) Arrow - Pointing Y+ (Up in World)
        // Cylinder Body
        val segments = 12
        val length = 1.0f
        val radius = 0.1f
        val headLength = 0.5f
        val headRadius = 0.25f
        
        // Cylinder
        for (i in 0 until segments) {
            val angle = (Math.PI * 2 * i / segments).toFloat()
            val nextAngle = (Math.PI * 2 * (i + 1) / segments).toFloat()
            
            // Side Quad as 2 triangles
            // V1
            addVertex(vertices, radius * cos(angle), -length, radius * sin(angle), 0.8f, 0.2f, 0.2f, 1f, cos(angle), 0f, sin(angle))
            addVertex(vertices, radius * cos(nextAngle), -length, radius * sin(nextAngle), 0.8f, 0.2f, 0.2f, 1f, cos(nextAngle), 0f, sin(nextAngle))
            addVertex(vertices, radius * cos(angle), length, radius * sin(angle), 0.8f, 0.2f, 0.2f, 1f, cos(angle), 0f, sin(angle))
            
            // V2
            addVertex(vertices, radius * cos(nextAngle), -length, radius * sin(nextAngle), 0.8f, 0.2f, 0.2f, 1f, cos(nextAngle), 0f, sin(nextAngle))
            addVertex(vertices, radius * cos(nextAngle), length, radius * sin(nextAngle), 0.8f, 0.2f, 0.2f, 1f, cos(nextAngle), 0f, sin(nextAngle))
            addVertex(vertices, radius * cos(angle), length, radius * sin(angle), 0.8f, 0.2f, 0.2f, 1f, cos(angle), 0f, sin(angle))
        }

        // Cone Head
        for (i in 0 until segments) {
            val angle = (Math.PI * 2 * i / segments).toFloat()
            val nextAngle = (Math.PI * 2 * (i + 1) / segments).toFloat()
            
            // Triangle Fan to Tip
            // Tip
            addVertex(vertices, 0f, length + headLength, 0f, 1.0f, 0.0f, 0.0f, 1f, 0f, 1f, 0f)
            // Base 1
            addVertex(vertices, headRadius * cos(angle), length, headRadius * sin(angle), 1.0f, 0.0f, 0.0f, 1f, cos(angle), 0f, sin(angle))
            // Base 2
            addVertex(vertices, headRadius * cos(nextAngle), length, headRadius * sin(nextAngle), 1.0f, 0.0f, 0.0f, 1f, cos(nextAngle), 0f, sin(nextAngle))
            
            // Disk cap at bottom of cone (optional, skipping for simplicity)
        }
        
        // South (Blue) Tail - Pointing Y-
        // Cylinder Body (Mirror of top, but Blue)
        // ... (Skipping full South tail for brevity, user focus is North/Orientation)
        // Adding simple Bottom Cap for North Arrow to close it
        for (i in 0 until segments) {
            val angle = (Math.PI * 2 * i / segments).toFloat()
            val nextAngle = (Math.PI * 2 * (i + 1) / segments).toFloat()
            
            addVertex(vertices, 0f, -length, 0f, 0.2f, 0.2f, 0.8f, 1f, 0f, -1f, 0f)
            addVertex(vertices, radius * cos(nextAngle), -length, radius * sin(nextAngle), 0.2f, 0.2f, 0.8f, 1f, 0f, -1f, 0f)
            addVertex(vertices, radius * cos(angle), -length, radius * sin(angle), 0.2f, 0.2f, 0.8f, 1f, 0f, -1f, 0f)
        }


        val floatArray = FloatArray(vertices.size)
        for (i in vertices.indices) {
            floatArray[i] = vertices[i]
        }
        
        vertexCount = vertices.size / 10 // 10 floats per vertex
        
        vertexBuffer = ByteBuffer.allocateDirect(floatArray.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(floatArray)
                position(0)
            }
        }
    }

    private fun addVertex(list: ArrayList<Float>, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float, nx: Float, ny: Float, nz: Float) {
        list.add(x); list.add(y); list.add(z)
        list.add(r); list.add(g); list.add(b); list.add(a)
        list.add(nx); list.add(ny); list.add(nz)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }
}
