uniform mat4 u_MVPMatrix;   // in: Matriz Projection*ModelView
uniform mat4 u_MVMatrix;	// in: Matriz ModelView
uniform vec4 u_Color;		// in: color del objeto

attribute vec4 a_Position;	// in: Posición de cada vértice
attribute vec3 a_Normal;	// in: Normal de cada vértice

varying vec4 v_Color;		// out: Color de salida al fragment shader

void main()
{
	vec3 LightPos = vec3(0, 1, -7);							// Posición de la luz [fija]
	//vec3 LightPos = vec3(u_MVMatrix*vec4(0, 1, -3, 1));	// Posición de la luz [rotando]
	vec3 P = vec3(u_MVMatrix * a_Position);					// Posición del vértice
	vec3 N = vec3(u_MVMatrix * vec4(a_Normal, 0.0));    	// Normal del vértice
	
	float d = length(LightPos - P);							// distancia
	vec3  L = normalize(LightPos - P);						// Vector Luz
	float diffuse = max(dot(N, L), 0.15);					// Cálculo de la intensidad difusa (15% de intensidad ambiente)
	float attenuation = 1.0/(0.3+(0.1*d)+(0.01*d*d)); 		// Cálculo de la atenuación
	diffuse = diffuse*attenuation;
	
	v_Color = u_Color * diffuse;

	gl_Position = u_MVPMatrix * a_Position;
}