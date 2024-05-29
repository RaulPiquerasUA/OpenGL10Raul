package com.japg.mastermoviles.opengl10.util;

import android.util.Log;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

public class ShaderHelper {
	private static final String TAG = "ShaderHelper";
	
	public static int compileVertexShader(String shaderCode) {
		return compileShader(GL_VERTEX_SHADER, shaderCode);
	}
	
	public static int compileFragmentShader(String shaderCode) {
		return compileShader(GL_FRAGMENT_SHADER, shaderCode);
	}
	
	private static int compileShader(int type, String shaderCode) {
		final int shaderObjectId = glCreateShader(type);
		
		if (shaderObjectId == 0) {
			if (LoggerConfig.ON) {
				Log.w(TAG, "No se pudeo crear un nuevo shader.");
			}
			return 0;
		}
		glShaderSource(shaderObjectId, shaderCode);
		glCompileShader(shaderObjectId);
		
		final int[] compileStatus = new int[1];
		glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);
		
		if (LoggerConfig.ON) {
			// Print the shader info log to the Android log output.
			Log.v(TAG, "Resultado de la compilación del código:" + "\n" + shaderCode + "\n");
				//+ glGetShaderInfoLog(shaderObjectId));
		}
		if (compileStatus[0] == 0) {
			// Si falla borramos el objeto shader.
			glDeleteShader(shaderObjectId);
			if (LoggerConfig.ON) {
				Log.w(TAG, "La compilación del shader ha fallado.");
			}
			return 0;
		}

		return shaderObjectId;
	}
	
	public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
		final int programObjectId = glCreateProgram();
		if (programObjectId == 0) {
			if (LoggerConfig.ON) {
				Log.w(TAG, "No se puede crear un nuevo programa");
			}
			return 0;
		}
		// Añadimos los shaders
		glAttachShader(programObjectId, vertexShaderId);
		glAttachShader(programObjectId, fragmentShaderId);
		// Enlazamos los shaders
		glLinkProgram(programObjectId);
		// Comprobamos el resultado
		final int[] linkStatus = new int[1];
		glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
		// Añadimos la traza al log
		if (LoggerConfig.ON) {
			Log.v(TAG, "Resultado del enlace del programa:\n");	//+ glGetProgramInfoLog(programObjectId));
		}
		// Comprobamos el resultados del enlace
		if (linkStatus[0] == 0) {
			// Si falla borra el objeto programa.
			glDeleteProgram(programObjectId);
			if (LoggerConfig.ON) {
				Log.w(TAG, "Ha fallado el enlazado del programa.");
			}
			return 0;
		}
		// Todo correcto, devolvemos el identificador
		return programObjectId;
	}
	
	public static boolean validateProgram(int programObjectId) {
		glValidateProgram(programObjectId);
		
		final int[] validateStatus = new int[1];
		glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
		
		Log.v(TAG, "Resultado de la validación del programa: " + validateStatus[0]
				+ "\nLog:\n");// + glGetProgramInfoLog(programObjectId));
		return validateStatus[0] != 0;
	}
}
