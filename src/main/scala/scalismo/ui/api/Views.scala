package scalismo.ui.api

import java.awt.Color

import breeze.linalg.DenseVector
import scalismo.common.{ DiscreteVectorField, DiscreteScalarField }
import scalismo.geometry.{ Dim, Landmark, Point, _3D }
import scalismo.image.DiscreteScalarImage
import scalismo.mesh.{ ScalarMeshField, TriangleMesh }
import scalismo.registration.RigidTransformation
import scalismo.statisticalmodel.{ DiscreteLowRankGaussianProcess, StatisticalMeshModel }
import scalismo.ui.model.SceneNode.event.{ ChildAdded, ChildRemoved }
import scalismo.ui.model._
import scalismo.ui.model.capabilities.Removeable
import scalismo.ui.model.properties.ScalarRange

/**
 * Created by luetma00 on 08.04.16.
 */
sealed trait ObjectView {
  type PeerType <: SceneNode with Removeable

  protected[api] def peer: PeerType

  def name: String = peer.name

  def inGroup: Group = {
    Group(findBelongingGroup(peer))
  }
  def remove(): Unit = peer.remove()

  private def findBelongingGroup(node: SceneNode): GroupNode = {
    if (node.isInstanceOf[Group]) this.asInstanceOf[GroupNode]
    else findBelongingGroup(node.parent)
  }
}

case class PointCloudView(override protected[api] val peer: PointCloudNode) extends ObjectView {
  type PeerType = PointCloudNode
  def color = peer.color.value
  def color_=(c: Color): Unit = { peer.color.value = c }
  def radius = peer.radius.value
  def radius_=(r: Float): Unit = { peer.radius.value = r }
  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }
}

object PointCloudView {
  implicit object FindInScenePointCloud extends FindInScene[PointCloudView] {
    override def createView(s: SceneNode): Option[PointCloudView] = {
      if (s.isInstanceOf[PointCloudNode]) Some(PointCloudView(s.asInstanceOf[PointCloudNode])) else None
    }
  }

  implicit def callbackPointCloudView = new HandleCallback[PointCloudView] {

    override def registerOnAdd[R](g: Group, f: PointCloudView => R): Unit = {
      g.peer.listenTo(g.peer.pointClouds)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val tmv = PointCloudView(newNode.asInstanceOf[PointCloudNode])
          f(tmv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: PointCloudView => R): Unit = {
      g.peer.listenTo(g.peer.pointClouds)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = PointCloudView(removedNode.asInstanceOf[PointCloudNode])
          f(tmv)
        }
      }
    }
  }
}

case class TriangleMeshView(override protected[api] val peer: TriangleMeshNode) extends ObjectView {
  type PeerType = TriangleMeshNode
  def color = peer.color.value
  def color_=(c: Color): Unit = { peer.color.value = c }
  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }

  def triangleMesh: TriangleMesh[_3D] = peer.source
  def transformedTriangleMesh: TriangleMesh[_3D] = peer.transformedSource
}

object TriangleMeshView {

  implicit object FindInSceneTriangleMeshView$ extends FindInScene[TriangleMeshView] {
    override def createView(s: SceneNode): Option[TriangleMeshView] = {
      if (s.isInstanceOf[TriangleMeshNode]) Some(TriangleMeshView(s.asInstanceOf[TriangleMeshNode])) else None
    }
  }

  implicit def callbackTriangleMeshView = new HandleCallback[TriangleMeshView] {

    override def registerOnAdd[R](g: Group, f: TriangleMeshView => R): Unit = {
      g.peer.listenTo(g.peer.triangleMeshes)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val tmv = TriangleMeshView(newNode.asInstanceOf[TriangleMeshNode])
          f(tmv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: TriangleMeshView => R): Unit = {
      g.peer.listenTo(g.peer.triangleMeshes)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = TriangleMeshView(removedNode.asInstanceOf[TriangleMeshNode])
          f(tmv)
        }
      }
    }

  }
}

case class LandmarkView(override protected[api] val peer: LandmarkNode) extends ObjectView {
  type PeerType = LandmarkNode

  def color = peer.color.value
  def color_=(c: Color): Unit = { peer.color.value = c }
  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }
  def landmark: Landmark[_3D] = peer.source
  def transformedLandmark: Landmark[_3D] = peer.transformedSource
}

object LandmarkView {
  implicit object FindInSceneLandmarkView$ extends FindInScene[LandmarkView] {
    override def createView(s: SceneNode): Option[LandmarkView] = {
      if (s.isInstanceOf[LandmarkNode]) Some(LandmarkView(s.asInstanceOf[LandmarkNode])) else None
    }
  }

  implicit object CallbackLandmarkView extends HandleCallback[LandmarkView] {

    override def registerOnAdd[R](g: Group, f: LandmarkView => R): Unit = {
      g.peer.listenTo(g.peer.landmarks)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val tmv = LandmarkView(newNode.asInstanceOf[LandmarkNode])
          f(tmv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: LandmarkView => R): Unit = {
      g.peer.listenTo(g.peer.landmarks)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = LandmarkView(removedNode.asInstanceOf[LandmarkNode])
          f(tmv)
        }
      }
    }
  }
}

case class ScalarMeshFieldView(override protected[api] val peer: ScalarMeshFieldNode) extends ObjectView {
  type PeerType = ScalarMeshFieldNode

  def scalarRange: ScalarRange = peer.scalarRange.value
  def scalarRange_=(s: ScalarRange): Unit = { peer.scalarRange.value = s }
  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }

  def scalarMeshField: ScalarMeshField[Float] = peer.source
  def transformedScalarMeshField = peer.transformedSource
}

object ScalarMeshFieldView {
  implicit object FindInSceneScalarMeshField extends FindInScene[ScalarMeshFieldView] {
    override def createView(s: SceneNode): Option[ScalarMeshFieldView] = {
      if (s.isInstanceOf[ScalarMeshFieldNode]) Some(ScalarMeshFieldView(s.asInstanceOf[ScalarMeshFieldNode])) else None
    }
  }

  implicit object CallbackScalarMeshFieldView extends HandleCallback[ScalarMeshFieldView] {

    override def registerOnAdd[R](g: Group, f: ScalarMeshFieldView => R): Unit = {
      g.peer.listenTo(g.peer.scalarMeshFields)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val tmv = ScalarMeshFieldView(newNode.asInstanceOf[ScalarMeshFieldNode])
          f(tmv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: ScalarMeshFieldView => R): Unit = {
      g.peer.listenTo(g.peer.scalarMeshFields)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = ScalarMeshFieldView(removedNode.asInstanceOf[ScalarMeshFieldNode])
          f(tmv)
        }
      }
    }
  }
}

case class ScalarFieldView(override protected[api] val peer: ScalarFieldNode) extends ObjectView {
  type PeerType = ScalarFieldNode

  def scalarRange: ScalarRange = peer.scalarRange.value
  def scalarRange_=(s: ScalarRange): Unit = { peer.scalarRange.value = s }
  def radius = peer.radius.value
  def radius_=(r: Float): Unit = { peer.radius.value = r }
  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }

  def scalarField: DiscreteScalarField[_3D, Float] = peer.source
  def transformedScalarField = peer.transformedSource
}

object ScalarFieldView {

  implicit object FindInSceneScalarMeshField extends FindInScene[ScalarFieldView] {
    override def createView(s: SceneNode): Option[ScalarFieldView] = {
      if (s.isInstanceOf[ScalarFieldNode]) Some(ScalarFieldView(s.asInstanceOf[ScalarFieldNode])) else None
    }
  }

  implicit object CallbackScalarFieldView extends HandleCallback[ScalarFieldView] {

    override def registerOnAdd[R](g: Group, f: ScalarFieldView => R): Unit = {
      g.peer.listenTo(g.peer.scalarFields)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val tmv = ScalarFieldView(newNode.asInstanceOf[ScalarFieldNode])
          f(tmv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: ScalarFieldView => R): Unit = {
      g.peer.listenTo(g.peer.scalarFields)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = ScalarFieldView(removedNode.asInstanceOf[ScalarFieldNode])
          f(tmv)
        }
      }
    }
  }
}

case class VectorFieldView(override protected[api] val peer: VectorFieldNode) extends ObjectView {
  type PeerType = VectorFieldNode

  def scalarRange: ScalarRange = peer.scalarRange.value
  def scalarRange_=(s: ScalarRange): Unit = { peer.scalarRange.value = s }
  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }

  def vectorField[A]: DiscreteVectorField[_3D, _3D] = peer.source
}

object VectorFieldView {

  implicit object FindInSceneScalarMeshField extends FindInScene[VectorFieldView] {
    override def createView(s: SceneNode): Option[VectorFieldView] = {
      if (s.isInstanceOf[VectorFieldNode]) Some(VectorFieldView(s.asInstanceOf[VectorFieldNode])) else None
    }
  }

  implicit object CallbackVectorFieldView extends HandleCallback[VectorFieldView] {

    override def registerOnAdd[R](g: Group, f: VectorFieldView => R): Unit = {
      g.peer.listenTo(g.peer.vectorFields)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val tmv = VectorFieldView(newNode.asInstanceOf[VectorFieldNode])
          f(tmv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: VectorFieldView => R): Unit = {
      g.peer.listenTo(g.peer.vectorFields)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = VectorFieldView(removedNode.asInstanceOf[VectorFieldNode])
          f(tmv)
        }
      }
    }
  }
}

case class ImageView(override protected[api] val peer: ImageNode) extends ObjectView {
  type PeerType = ImageNode

  def opacity = peer.opacity.value
  def opacity_=(o: Float): Unit = { peer.opacity.value = o }

  def image: DiscreteScalarImage[_3D, Float] = peer.source
}

object ImageView {
  implicit object FindImage extends FindInScene[ImageView] {
    override def createView(s: SceneNode): Option[ImageView] = {
      if (s.isInstanceOf[ImageView]) Some(ImageView(s.asInstanceOf[ImageNode])) else None
    }
  }

  implicit object CallbackLandmarkView extends HandleCallback[ImageView] {

    override def registerOnAdd[R](g: Group, f: ImageView => R): Unit = {
      g.peer.listenTo(g.peer.images)
      g.peer.reactions += {
        case ChildAdded(collection, newNode) => {
          val imv = ImageView(newNode.asInstanceOf[ImageNode])
          f(imv)
        }
      }
    }

    override def registerOnRemove[R](g: Group, f: ImageView => R): Unit = {
      g.peer.listenTo(g.peer.images)
      g.peer.reactions += {
        case ChildRemoved(collection, removedNode) => {
          val tmv = ImageView(removedNode.asInstanceOf[ImageNode])
          f(tmv)
        }
      }
    }
  }

}

// Note this class does not extend Object view, as there is not really a corresponding node to this concept
case class StatisticalMeshModelView(private val meshNode: TriangleMeshNode,
    private val shapeTransNode: TransformationNode[DiscreteLowRankGpPointTransformation],
    private val poseTransNode: TransformationNode[RigidTransformation[_3D]]) {

  def meshView = TriangleMeshView(meshNode)
  def shapeTransformationView = DiscreteLowRankGPTransformationView(shapeTransNode)
  def poseTransformationView = RigidTransformationView(poseTransNode)

  def statisticalMeshModel: StatisticalMeshModel = StatisticalMeshModel(meshView.triangleMesh, shapeTransNode.transformation.dgp)
}

case class Group(override protected[api] val peer: GroupNode) extends ObjectView {

  type PeerType = GroupNode
}

object Group {
  implicit object FindInSceneGroup$ extends FindInScene[Group] {
    override def createView(s: SceneNode): Option[Group] = {
      if (s.isInstanceOf[GroupNode]) Some(Group(s.asInstanceOf[GroupNode])) else None
    }
  }

}

case class TransformationView(override protected[api] val peer: TransformationNode[Point[_3D] => Point[_3D]]) extends ObjectView {
  def transformation: Point[_3D] => Point[_3D] = peer.transformation
  def transformation_=(t: Point[_3D] => Point[_3D]): Unit = { peer.transformation = t }

  override type PeerType = TransformationNode[Point[_3D] => Point[_3D]]

}

object TransformationView {
  implicit object FindInSceneGenericTransformation$ extends FindInScene[TransformationView] {
    override def createView(s: SceneNode): Option[TransformationView] = {

      type PointToPointTransformation[D <: Dim] = Point[D] => Point[D]
      // here we need a two step process due to type erasure to find the right type.
      if (s.isInstanceOf[TransformationNode[_]]
        && s.asInstanceOf[TransformationNode[_]].transformation.isInstanceOf[PointToPointTransformation[_]]) {
        Some(TransformationView(s.asInstanceOf[TransformationNode[PointToPointTransformation[_3D]]]))
      } else None
    }
  }
}

case class RigidTransformationView(override protected[api] val peer: TransformationNode[RigidTransformation[_3D]]) extends ObjectView {

  override type PeerType = TransformationNode[RigidTransformation[_3D]]

  def transformation: RigidTransformation[_3D] = peer.transformation
  def transformation_=(transformation: RigidTransformation[_3D]): Unit = {
    peer.transformation = transformation
  }
}

object RigidTransformationView {

  implicit object FindInSceneRigidTransformation$ extends FindInScene[RigidTransformationView] {
    override def createView(s: SceneNode): Option[RigidTransformationView] = {

      // here we need a two step process due to type erasure to find the right type.
      if (s.isInstanceOf[TransformationNode[_]]
        && s.asInstanceOf[TransformationNode[_]].transformation.isInstanceOf[RigidTransformation[_]]) {
        Some(RigidTransformationView(s.asInstanceOf[TransformationNode[RigidTransformation[_3D]]]))
      } else None
    }
  }

}

case class DiscreteLowRankGPTransformationView(override protected[api] val peer: TransformationNode[DiscreteLowRankGpPointTransformation]) extends ObjectView {

  override type PeerType = TransformationNode[DiscreteLowRankGpPointTransformation]

  def coefficients: DenseVector[Double] = peer.transformation.coefficients
  def coefficients_=(coefficients: DenseVector[Double]): Unit = {
    { peer.transformation = peer.transformation.copy(coefficients) }
  }

  val transformation: Point[_3D] => Point[_3D] = peer.transformation

  def discreteLowRankGaussianProcess = peer.transformation.dgp
  def discreteLowRankGaussianProcess_=(dgp: DiscreteLowRankGaussianProcess[_3D, _3D]): Unit = {
    peer.transformation = (DiscreteLowRankGpPointTransformation(dgp))
  }
}

object DiscreteLowRankGPTransformationView {
  implicit object FindInSceneDiscreteGPTransformation$ extends FindInScene[DiscreteLowRankGPTransformationView] {
    override def createView(s: SceneNode): Option[DiscreteLowRankGPTransformationView] = {

      // here we need a two step process due to type erasure to find the right type.
      if (s.isInstanceOf[TransformationNode[_]]
        && s.asInstanceOf[TransformationNode[_]].transformation.isInstanceOf[DiscreteLowRankGpPointTransformation]) {
        Some(DiscreteLowRankGPTransformationView(s.asInstanceOf[TransformationNode[DiscreteLowRankGpPointTransformation]]))
      } else None
    }
  }

}

case class LowRankGPTransformationView(override protected[api] val peer: TransformationNode[LowRankGpPointTransformation]) extends ObjectView {

  override type PeerType = TransformationNode[LowRankGpPointTransformation]

  def coefficients: DenseVector[Double] = peer.transformation.coefficients
  def coefficients_=(coefficients: DenseVector[Double]): Unit = {
    { peer.transformation = peer.transformation.copy(coefficients) }
  }
}
