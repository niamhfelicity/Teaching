package com.bentonian.framework.mesh.primitive;

import static java.lang.Math.abs;

import com.bentonian.framework.math.M3d;
import com.bentonian.framework.math.MathConstants;
import com.bentonian.framework.math.Ray;
import com.bentonian.framework.math.RayIntersections;
import com.bentonian.framework.mesh.Face;
import com.bentonian.framework.mesh.Vertex;
import com.bentonian.framework.scene.IsRayTraceable;
import com.bentonian.framework.texture.TexCoord;

public class Cube extends MeshPrimitiveWithTexture implements IsRayTraceable {

  private static final M3d X_AXIS = new M3d(1, 0, 0);
  private static final M3d Z_AXIS = new M3d(0, 0, 1);

  public Cube() {
    Vertex[] vertices = new Vertex[8];
    for (int i = 0; i < 8; i++) {
      vertices[i] = new Vertex(MathConstants.CORNERS_OF_A_CUBE[i]);
    }
    for (int face = 0; face < 6; face++) {
      getMesh().add(new Face(
          vertices[MathConstants.INDICES_OF_A_CUBE[face][0]],
          vertices[MathConstants.INDICES_OF_A_CUBE[face][1]],
          vertices[MathConstants.INDICES_OF_A_CUBE[face][2]],
          vertices[MathConstants.INDICES_OF_A_CUBE[face][3]]
      ));
    }
  }

  @Override
  public RayIntersections traceLocal(Ray ray) {
    RayIntersections hitList = new RayIntersections();
    collide(hitList, -(ray.origin.getX()-1) / ray.direction.getX(), ray);
    collide(hitList, -(ray.origin.getX()+1) / ray.direction.getX(), ray);
    collide(hitList, -(ray.origin.getY()-1) / ray.direction.getY(), ray);
    collide(hitList, -(ray.origin.getY()+1) / ray.direction.getY(), ray);
    collide(hitList, -(ray.origin.getZ()-1) / ray.direction.getZ(), ray);
    collide(hitList, -(ray.origin.getZ()+1) / ray.direction.getZ(), ray);
    return hitList;
  }

  private boolean collide(RayIntersections hitList, double t, Ray ray) {
    if (t > MathConstants.EPSILON) {
      M3d pt = ray.at(t);
      if (Math.abs(pt.getX()) <= 1.00001 &&
          Math.abs(pt.getY()) <= 1.00001 &&
          Math.abs(pt.getZ()) <= 1.00001) {
        hitList.add(this, t, pt, getNormal(pt), getMaterial(pt));
      }
    }

    return false;
  }

  private M3d getNormal(M3d pt) {
    return applyTextureToNormal(pt, pt.toAxis());
  }

  @Override
  public TexCoord getTextureCoord(M3d pt) {
    return getTextureCoord(pt, getNormal(pt));
  }

  private TexCoord getTextureCoord(M3d pt, M3d normal) {
    double u, v;

    if (normal.getX() > 0.99999) {
      u = (1 - pt.getZ()) / 2.0;
      v = (1 - pt.getY()) / 2.0;
    } else if (normal.getX() < -0.99999) {
      u = (pt.getZ() + 1) / 2.0;
      v = (1 - pt.getY()) / 2.0;
    } else if (normal.getZ() > 0.99999) {
      u = (pt.getX() + 1) / 2.0;
      v = (1 - pt.getY()) / 2.0;
    } else if (normal.getZ() < -0.99999) {
      u = (1 - pt.getX()) / 2.0;
      v = (1 - pt.getY()) / 2.0;
    } else if (normal.getY() > 0.99999) {
      u = (pt.getX() + 1) / 2.0;
      v = (pt.getZ() + 1) / 2.0;
    } else {
      u = (pt.getX() + 1) / 2.0;
      v = (1 - pt.getZ()) / 2.0;
    }

    return new TexCoord(u, v);
  }

  @Override
  public M3d getUBasis(M3d pt) {
    if (abs(abs(pt.getX()) - 1) < MathConstants.EPSILON) {
      return Z_AXIS;
    } else {
      return X_AXIS;
    }
  }

  @Override
  protected void renderVertex(Face face, int index) {
    textureCoordinates(getTextureCoord(face.get(index), face.getNormal()));
    color(getMaterial(face.get(index)).getColor());
    vertex(face.get(index));
  }
}
