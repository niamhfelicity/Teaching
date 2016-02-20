#version 330

#include "common.fsh"
#include "noise3D.fsh"

#define SPHERE 0
#define BOX 1

const int renderDepth = 400;
const vec3 lightPos = vec3(0, 10, 10);

vec3 scene(vec3 rayorig, vec3 raydir);

vec2 f(vec3 pt) {
  float s = sdSphere(pt - vec3(-0.5,0,0), 0.75);
s = smin(s, sdCube(pt - vec3(0.5,0,0), vec3(0.5)));
  float c = sdCube(pt - vec3(0, -0.75, 0), vec3(2, 0.25, 2));
  return (abs(s) < abs(c)) ? vec2(s, SPHERE) : vec2(c, BOX);
}

vec3 gradient(vec3 pt) {
  vec3 off = vec3(0.0001, 0, 0);
  return vec3(
    f(pt + off.xyz).x - f(pt - off.xyz).x,
    f(pt + off.yxz).x - f(pt - off.yxz).x,
    f(pt + off.yzx).x - f(pt - off.yzx).x);
}

vec4 raymarch(vec3 rayorig, vec3 raydir) {
  int step = 0;
  vec3 pos = rayorig;
  float d = f(pos).x;

  while (abs(d) > 0.001 && step < renderDepth) {
    pos = pos + raydir * abs(d);
    d = f(pos).x;
    step++;
  }

  return vec4(pos, float(step));
}

vec4 color(int which, vec3 pt) {
  switch (which) {
    default:
    case BOX:
      int index = int(floor(pt.x)) + int(floor(pt.y)) + int(floor(pt.z));
      return vec4((((index & 0x01) == 0) ? black : white), 1);
    case SPHERE:
      return vec4(blue, 0.5);
  }
}

float illuminate(vec3 pt, vec3 eye, vec3 normal) {
  float diff = diffuse(pt.xyz, normal, lightPos);
  float spec = specular(pt.xyz, normal, lightPos, eye);
  return 0.25 + diff + spec;
}

vec3 tbdSrc[10], tbdDir[10];
float tbdWeight[10];
int numTbd = 0;

void addTbd(vec3 rayorig, vec3 raydir, float weight) {
  if (numTbd < tbdSrc.length() - 1) {
    tbdSrc[numTbd] = rayorig;
    tbdDir[numTbd] = raydir;
    tbdWeight[numTbd] = weight;
    numTbd++;
  }
}

vec3 scene(vec3 rayorig, vec3 raydir) {
  vec3 cumulativeColor = vec3(0);

  addTbd(rayorig, raydir, 1.0);
  for (int i = 0; i < 10 && numTbd > 0; i++) {
    vec3 src = tbdSrc[numTbd - 1];
    vec3 dir = tbdDir[numTbd - 1];
    float weight = tbdWeight[numTbd - 1];
    numTbd--;

    vec4 res = raymarch(src, dir);
    if (res.w < renderDepth) {
      vec2 hitSurface = f(res.xyz);
      vec4 col = color(int(hitSurface.y), res.xyz);
      vec3 normal = normalize(gradient(res.xyz));
      cumulativeColor += weight * col.w * illuminate(res.xyz, src, normal) * col.xyz;
      if (col.w < 1) {
        bool comingFromOutside = (hitSurface.x >= 0);
        float refractiveIndexOutside = comingFromOutside ? 1.3 : 1.000277;
        float refractiveIndexInside = comingFromOutside ? 1.000277 : 1.3;
        vec3 refractDir = normalize(refract(dir, comingFromOutside ? normal : -normal, refractiveIndexInside / refractiveIndexOutside));
        addTbd(
            res.xyz + 0.01 * refractDir,
            refractDir,
            weight * (1 - col.w));
      }
    } else {
      cumulativeColor += weight * background;
    }
  }
  return cumulativeColor;
}

void main() {
  fragColor = vec4(scene(iRayOrigin, getRayDir(iRayDir, iRayUp, texCoord)), 1.0);
}