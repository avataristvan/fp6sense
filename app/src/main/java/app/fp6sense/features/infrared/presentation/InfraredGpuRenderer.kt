package app.fp6sense.features.infrared.presentation

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class InfraredGpuRenderer : GLSurfaceView.Renderer {

    // Vertex shader (Pass-through)
    private val vertexShaderCode = """#version 300 es
        in vec4 vPosition;
        in vec2 vTexCoord;
        out vec2 uv;
        void main() {
            gl_Position = vPosition;
            uv = vTexCoord;
        }
    """.trimIndent()

    // Fragment shader: Decodes 16-bit data and maps spectrum
    private val fragmentShaderCode = """#version 300 es
        precision mediump float;
        in vec2 uv;
        uniform sampler2D sTexture; // RG texture (Bytes: R=LSB, G=MSB)
        uniform vec2 uStep;
        out vec4 fragColor;

        void main() {
            // Snap UV to 2x2 Bayer block (Red is typically Top-Left in RGGB)
            vec2 snappedUv = floor(uv / (uStep * 2.0)) * (uStep * 2.0);
            
            // Texture is GL_RG (Red, Green components).
            // We treat 16-bit pixel as 2 bytes: LSB (R) and MSB (G).
            // texture() returns normalized 0..1 floats.
            vec2 data = texture(sTexture, snappedUv).rg;
            
            // Reconstruct 16-bit value
            // data.r = LSB byte (0..1)
            // data.g = MSB byte (0..1)
            // Value = MSB * 256 + LSB
            // Normalized: Value / 65535 = (MSB * 256/255 + LSB/255) / 257? No.
            // Simplified: Value = data.g + data.r / 256.0; (Approximation 0..1)
            
            float intensity = data.g + (data.r / 256.0);
            
            // Normalize assuming 10-bit data (0..1023) in 16-bit container (0..65535)
            // 65535 / 1023 ≈ 64.0
            intensity = intensity * 64.0; 
            intensity = clamp(intensity, 0.0, 1.0);

            // False-color mapping: Red -> Orange -> Violet
            vec3 color;
            if (intensity < 0.5) {
                float t = intensity / 0.5;
                color = mix(vec3(1.0, 0.0, 0.0), vec3(1.0, 0.5, 0.0), t);
            } else {
                float t = (intensity - 0.5) / 0.5;
                color = mix(vec3(1.0, 0.5, 0.0), vec3(0.7, 0.0, 1.0), t);
            }
            fragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    private var program = 0
    private var textureId = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private val vertices = floatArrayOf(
        -1f, -1f, 0f,
         1f, -1f, 0f,
        -1f,  1f, 0f,
         1f,  1f, 0f
    )

    // Vertically flipped texture coordinates
    private val texCoords = floatArrayOf(
        1f, 1f,
        1f, 0f,
        0f, 1f,
        0f, 0f
    )

    @Volatile
    var rawBuffer: ByteBuffer? = null
    var width = 0
    var height = 0
    var rowStride = 0 // Stride in BYTES

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(texCoords)
                position(0)
            }
        }

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram().apply {
            GLES30.glAttachShader(this, vertexShader)
            GLES30.glAttachShader(this, fragmentShader)
            GLES30.glLinkProgram(this)
        }

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        
        // Nearest neighbor to preserve Bayer structure for sampling
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        
        val buffer = rawBuffer ?: return
        if (width <= 0 || height <= 0 || rowStride <= 0) return

        GLES30.glUseProgram(program)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        // Set Unpack alignment to 1 byte (tight) just in case
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        
        // Handling Row Stride using GLES 3.0
        // rowStride is in BYTES.
        // We are uploading as GL_RG (2 bytes per pixel, GL_UNSIGNED_BYTE).
        // So Stride in Pixels (for GL) = rowStride / 2 bytes.
        val strideInPixels = rowStride / 2
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, strideInPixels)

        buffer.position(0)
        
        // Upload as GL_RG (Two bytes per pixel: R=Byte1, G=Byte2)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG8, 
            width, height, 0, GLES30.GL_RG, GLES30.GL_UNSIGNED_BYTE, buffer
        )
        
        // Reset unpack row length to avoid side effects
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0)

        val posHandle = GLES30.glGetAttribLocation(program, "vPosition")
        GLES30.glEnableVertexAttribArray(posHandle)
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 12, vertexBuffer)

        val texHandle = GLES30.glGetAttribLocation(program, "vTexCoord")
        GLES30.glEnableVertexAttribArray(texHandle)
        GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, 8, texCoordBuffer)

        val stepHandle = GLES30.glGetUniformLocation(program, "uStep")
        GLES30.glUniform2f(stepHandle, 1.0f / width, 1.0f / height)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(posHandle)
        GLES30.glDisableVertexAttribArray(texHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }
}
