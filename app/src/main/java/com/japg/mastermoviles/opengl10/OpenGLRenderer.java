package com.japg.mastermoviles.opengl10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.japg.mastermoviles.opengl10.util.LoggerConfig;
import com.japg.mastermoviles.opengl10.util.Resource3DSReader;
import com.japg.mastermoviles.opengl10.util.ShaderHelper;
import com.japg.mastermoviles.opengl10.util.TextResourceReader;
import com.japg.mastermoviles.opengl10.util.TextureHelper;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.*;
import static android.opengl.Matrix.*;

public class OpenGLRenderer implements Renderer {
	private static final String TAG = "OpenGLRenderer";

	private float scaleFactor = 1.0f;
	private float headRotationY = 0f;  // Rotación de la cabeza

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public void setHeadRotationY(float rotationY) {
		this.headRotationY = rotationY;
	}

	private static final int BYTES_PER_FLOAT = 4;
	private final Context context;
	private int program;

	private static final String U_MVPMATRIX = "u_MVPMatrix";
	private static final String U_MVMATRIX = "u_MVMatrix";
	private static final String U_COLOR = "u_Color";
	private static final String U_TEXTURE = "u_TextureUnit";

	private static final String A_POSITION = "a_Position";
	private static final String A_NORMAL = "a_Normal";
	private static final String A_UV = "a_UV";

	private int uMVPMatrixLocation;
	private int uMVMatrixLocation;
	private int uColorLocation;
	private int uTextureUnitLocation;
	private int aPositionLocation;
	private int aNormalLocation;
	private int aUVLocation;

	private int texture;

	private float previousX = 0f;
	private float previousY = 0f;
	private float accumulatedRX = 0f;
	private float accumulatedRY = 0f;

	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int NORMAL_COMPONENT_COUNT = 3;
	private static final int UV_COMPONENT_COUNT = 2;
	private static final int STRIDE =
			(POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + UV_COMPONENT_COUNT) * BYTES_PER_FLOAT;

	private final float[] projectionMatrix = new float[16];
	private final float[] modelMatrixBody = new float[16];
	private final float[] modelMatrixHead = new float[16];
	private final float[] MVP = new float[16];

	private Resource3DSReader obj3DSBody;
	private Resource3DSReader obj3DSHead;

	void perspective(float[] m, int offset, float fovy, float aspect, float n, float f) {
		final float d = f - n;
		final float angleInRadians = (float) (fovy * Math.PI / 180.0);
		final float a = (float) (1.0 / Math.tan(angleInRadians / 2.0));

		m[0] = a / aspect;
		m[1] = 0f;
		m[2] = 0f;
		m[3] = 0f;

		m[4] = 0f;
		m[5] = a;
		m[6] = 0f;
		m[7] = 0f;

		m[8] = 0;
		m[9] = 0;
		m[10] = (n - f) / d;
		m[11] = -1f;

		m[12] = 0f;
		m[13] = 0f;
		m[14] = -2 * f * n / d;
		m[15] = 0f;
	}

	public OpenGLRenderer(Context context) {
		this.context = context;
		obj3DSBody = new Resource3DSReader();
		obj3DSBody.read3DSFromResource(context, R.raw.cuerpo);
		obj3DSHead = new Resource3DSReader();
		obj3DSHead.read3DSFromResource(context, R.raw.cabeza);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		String vertexShaderSource;
		String fragmentShaderSource;

		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		int[] maxVertexTextureImageUnits = new int[1];
		int[] maxTextureImageUnits = new int[1];

		glGetIntegerv(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, maxVertexTextureImageUnits, 0);
		if (LoggerConfig.ON) {
			Log.w(TAG, "Max. Vertex Texture Image Units: " + maxVertexTextureImageUnits[0]);
		}
		glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureImageUnits, 0);
		if (LoggerConfig.ON) {
			Log.w(TAG, "Max. Texture Image Units: " + maxTextureImageUnits[0]);
		}
		texture = TextureHelper.loadTexture(context, R.drawable.green);

		if (maxVertexTextureImageUnits[0] > 0) {
			vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_vertex_shader);
			fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_fragment_shader);
		} else {
			vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_vertex_shader2);
			fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_fragment_shader2);
		}

		int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
		int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

		program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

		if (LoggerConfig.ON) {
			ShaderHelper.validateProgram(program);
		}

		glUseProgram(program);

		uMVPMatrixLocation = glGetUniformLocation(program, U_MVPMATRIX);
		uMVMatrixLocation = glGetUniformLocation(program, U_MVMATRIX);
		uColorLocation = glGetUniformLocation(program, U_COLOR);
		uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE);

		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		glEnableVertexAttribArray(aPositionLocation);
		aNormalLocation = glGetAttribLocation(program, A_NORMAL);
		glEnableVertexAttribArray(aNormalLocation);
		aUVLocation = glGetAttribLocation(program, A_UV);
		glEnableVertexAttribArray(aUVLocation);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		glViewport(0, 0, width, height);
		final float aspectRatio = width > height ? (float) width / (float) height : (float) height / (float) width;
		if (width > height) {
			perspective(projectionMatrix, 0, 45f, aspectRatio, 0.01f, 1000f);
		} else {
			perspective(projectionMatrix, 0, 45f, 1f / aspectRatio, 0.01f, 1000f);
		}
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glLineWidth(2.0f);

		// Dibujar el cuerpo
		setIdentityM(modelMatrixBody, 0);
		translateM(modelMatrixBody, 0, 0f, 0f, -20.0f * scaleFactor);
		rotateM(modelMatrixBody, 0, accumulatedRY, 0f, 1f, 0f);
		rotateM(modelMatrixBody, 0, accumulatedRX, 1f, 0f, 0f);
		multiplyMM(MVP, 0, projectionMatrix, 0, modelMatrixBody, 0);

		glUniformMatrix4fv(uMVPMatrixLocation, 1, false, MVP, 0);
		glUniformMatrix4fv(uMVMatrixLocation, 1, false, modelMatrixBody, 0);
		glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture);
		glUniform1f(uTextureUnitLocation, 0);

		for (int i = 0; i < obj3DSBody.numMeshes; i++) {
			Buffer buffer = obj3DSBody.dataBuffer[i];
			int vertexCount = obj3DSBody.numVertices[i];
			buffer.position(0);
			glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, buffer);
			buffer.position(POSITION_COMPONENT_COUNT);
			glVertexAttribPointer(aNormalLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, buffer);
			buffer.position(POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT);
			glVertexAttribPointer(aUVLocation, UV_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, buffer);
			glDrawArrays(GL_TRIANGLES, 0, vertexCount);
		}

		// Dibujar la cabeza
		setIdentityM(modelMatrixHead, 0);
		multiplyMM(modelMatrixHead, 0, modelMatrixBody, 0, modelMatrixHead, 0);
		translateM(modelMatrixHead, 0, 0f, 0.5f, 0f);
		rotateM(modelMatrixHead, 0, headRotationY, 0f, 0f, 1f);
		multiplyMM(MVP, 0, projectionMatrix, 0, modelMatrixHead, 0);

		glUniformMatrix4fv(uMVPMatrixLocation, 1, false, MVP, 0);
		glUniformMatrix4fv(uMVMatrixLocation, 1, false, modelMatrixHead, 0);
		glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

		for (int i = 0; i < obj3DSHead.numMeshes; i++) {
			Buffer buffer = obj3DSHead.dataBuffer[i];
			int vertexCount = obj3DSHead.numVertices[i];
			buffer.position(0);
			glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, buffer);
			buffer.position(POSITION_COMPONENT_COUNT);
			glVertexAttribPointer(aNormalLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, buffer);
			buffer.position(POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT);
			glVertexAttribPointer(aUVLocation, UV_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, buffer);
			glDrawArrays(GL_TRIANGLES, 0, vertexCount);
		}
	}

	public void handleTouchPress(float normalizedX, float normalizedY) {
		previousX = normalizedX;
		previousY = normalizedY;
	}

	public void handleTouchDrag(float normalizedX, float normalizedY) {
		float deltaX = normalizedX - previousX;
		float deltaY = normalizedY - previousY;

		accumulatedRX -= deltaY * 180f;
		accumulatedRY += deltaX * 180f;

		previousX = normalizedX;
		previousY = normalizedY;
	}
}
