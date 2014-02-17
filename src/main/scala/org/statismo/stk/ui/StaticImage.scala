package org.statismo.stk.ui

import org.statismo.stk.core.mesh.TriangleMesh
import java.io.File
import scala.util.Try
import org.statismo.stk.core.io.MeshIO
import org.statismo.stk.core.geometry.Point3D
import org.statismo.stk.core.io.ImageIO
import org.statismo.stk.core.image.DiscreteScalarImage3D
import org.statismo.stk.core.common.ScalarValue
import scala.reflect.ClassTag
import reflect.runtime.universe.{ TypeTag, typeOf }

object StaticImage extends SceneTreeObjectFactory[StaticImage[_]] with FileIoMetadata {
  val description = "Static 3D Image"
  val fileExtensions = Seq("nii", "nia")
  val metadata = this
  
  def apply(file: File)(implicit scene: Scene): Try[StaticImage[_]] = {
      apply(file, None, file.getName())
  }
  
  def apply(file: File, parent: Option[ThreeDRepresentations], name: String)(implicit scene: Scene): Try[StaticImage[_]] = {
    for {
      raw <- ImageIO.read3DScalarImage[Short](file)
    } yield {
      val static = new StaticImage(raw, parent, name)
      static
    }
  }
}

class StaticImage[A: ScalarValue : TypeTag: ClassTag](override val peer: DiscreteScalarImage3D[A], initialParent: Option[ThreeDRepresentations] = None, initialName: String = "(no name)")(implicit override val scene: Scene) extends ThreeDImage[A] {
  name = initialName
  
  override lazy val parent: ThreeDRepresentations = initialParent.getOrElse {
    val p = new StaticThreeDObject(Some(scene.statics), initialName)
    p.representations
  }
  parent.add(this)
  
  def addLandmarkAt(point: Point3D) = {
    val landmarks = parent.parent.landmarks
    landmarks.addAt(point)
  }
}