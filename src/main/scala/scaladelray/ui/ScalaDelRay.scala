/*
 * Copyright 2013 Stephan Rehfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scaladelray.ui

import scala.swing._
import scala.swing.GridBagPanel.{Anchor, Fill}
import javax.swing.table.{TableCellEditor, TableModel}
import scaladelray.ui.model._
import javax.swing._
import javax.swing.tree.{TreePath, TreeModel, TreeSelectionModel}
import javax.swing.event.{TableModelEvent, TableModelListener, TreeSelectionEvent, TreeSelectionListener}
import java.awt.event._
import scaladelray.Constants
import scala.swing.TabbedPane.Page
import scala.collection.mutable
import akka.actor._
import scala.concurrent.duration._
import java.net.{SocketException, DatagramPacket, DatagramSocket, InetAddress}
import scala.language.reflectiveCalls
import scaladelray.math.Vector3
import scaladelray.math.Point3
import scala.Some
import scala.swing.Action
import scala.swing.event.ButtonClicked
import scaladelray.Color
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.async.Async.async
import scala.util.Random

case class StartDiscovery()

object ScalaDelRay extends SimpleSwingApplication {

  var worldProvider = createStandardScene
  var renderingWindowsSize = new Dimension( 640, 480 )
  var recursionDepth = 10
  var clusterNodes = mutable.ListBuffer[(String,Int,Int)]()
  private val actorSystem = ActorSystem("ui")

  lazy val ui : GridBagPanel with TableModelListener = new GridBagPanel with TableModelListener {


    def tableChanged(e: TableModelEvent) {
      updateUI( None )
    }

    val c = new Constraints
    val shouldFill = true

    if( shouldFill ) c.fill = Fill.Horizontal

    val newPlaneButton = new Button {
      text = "Plane"
      reactions += {
        case ButtonClicked(_) =>
          val pp = new PlaneProvider
          detailsTable.model = pp
          val node = sceneGraphTree.getLastSelectedPathComponent
          if( node != null ) {
            node match {
              case np : NodeProvider =>
                np.childNodes += pp

              case _ =>
                worldProvider.geometryProvider += pp

            }
          } else {
            worldProvider.geometryProvider += pp

          }
          updateUI( Some( pp ) )
      }
    }

    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 0
    c.gridy = 0
    layout( newPlaneButton ) = c

    val newSphereButton = new Button {
      text = "Sphere"
      reactions += {
        case ButtonClicked(_) =>
          val sp = new SphereProvider
          detailsTable.model = sp
          val node = sceneGraphTree.getLastSelectedPathComponent
          if( node != null ) {
            node match {
              case np : NodeProvider =>
                np.childNodes += sp

              case _ =>
                worldProvider.geometryProvider += sp

            }
          } else {
            worldProvider.geometryProvider += sp

          }
          updateUI( Some( sp ) )
      }
    }
    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 1
    c.gridy = 0
    layout( newSphereButton ) = c

    val newBoxButton = new Button {
      text = "Box"
      reactions += {
        case ButtonClicked(_) =>
          val sp = new AxisAlignedBoxProvider
          detailsTable.model = sp
          val node = sceneGraphTree.getLastSelectedPathComponent
          if( node != null ) {
            node match {
              case np : NodeProvider =>
                np.childNodes += sp

              case _ =>
                worldProvider.geometryProvider += sp

            }
          } else {
            worldProvider.geometryProvider += sp

          }
          updateUI( Some( sp ) )
      }
    }
    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 2
    c.gridy = 0
    layout( newBoxButton ) = c

    val newTriangleButton = new Button {
      text = "Triangle"
      reactions += {
        case ButtonClicked(_) =>
          val sp = new TriangleProvider
          detailsTable.model = sp
          val node = sceneGraphTree.getLastSelectedPathComponent
          if( node != null ) {
            node match {
              case np : NodeProvider =>
                np.childNodes += sp

              case _ =>
                worldProvider.geometryProvider += sp

            }
          } else {
            worldProvider.geometryProvider += sp

          }
          updateUI( Some( sp ) )
      }
    }

    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 3
    c.gridy = 0
    layout( newTriangleButton ) = c

    val newNodeButton = new Button {
      text = "Node"
      reactions += {
        case ButtonClicked(_) =>
          val sp = new NodeProvider
          detailsTable.model = sp
          val node = sceneGraphTree.getLastSelectedPathComponent
          if( node != null ) {
            node match {
              case np : NodeProvider =>
                np.childNodes += sp

              case _ =>
                worldProvider.geometryProvider += sp

            }
          } else {
            worldProvider.geometryProvider += sp

          }
          updateUI( Some( sp ) )
      }
    }

    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 4
    c.gridy = 0
    layout( newNodeButton ) = c

    val newModelButton : Button = new Button {
      text = "Model"
      reactions += {
        case ButtonClicked(_) =>
          val sp = new ModelProvider( ui )
          detailsTable.model = sp
          val node = sceneGraphTree.getLastSelectedPathComponent
          if( node != null ) {
            node match {
              case np : NodeProvider =>
                np.childNodes += sp

              case _ =>
                worldProvider.geometryProvider += sp

            }
          } else {
            worldProvider.geometryProvider += sp

          }
          updateUI( Some( sp ) )
      }
    }
    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 5
    c.gridy = 0
    layout( newModelButton ) = c

    val lights = new GridBagPanel {
      val c = new Constraints

      val newPointLightButton = new Button {
        text = "Point Light"
        reactions += {
          case ButtonClicked(_) =>
            val sp = new PointLightProvider
            detailsTable.model = sp
            worldProvider.lightDescriptionProvider += sp
            updateUI( Some( sp ) )
        }
      }
      c.fill = Fill.Horizontal
      c.weightx = 0.5
      c.gridx = 0
      c.gridy = 1
      layout( newPointLightButton ) = c

      val newSpotLightButton = new Button {
        text = "Spot Light"
        reactions += {
          case ButtonClicked(_) =>
            val sp = new SpotLightProvider
            detailsTable.model = sp
            worldProvider.lightDescriptionProvider += sp
            updateUI( Some( sp ) )
        }
      }
      c.fill = Fill.Horizontal
      c.weightx = 0.5
      c.gridx = 1
      c.gridy = 1
      layout( newSpotLightButton ) = c

      val newDirectionalLightButton = new Button {
        text = "Directional Light"
        reactions += {
          case ButtonClicked(_) =>
            val sp = new DirectionalLightProvider
            detailsTable.model = sp
            worldProvider.lightDescriptionProvider += sp
            updateUI( Some( sp ) )
        }
      }
      c.fill = Fill.Horizontal
      c.weightx = 0.5
      c.gridx = 2
      c.gridy = 1
      layout( newDirectionalLightButton ) = c

      val newAreaLightButton = new Button {
        text = "Area light"
        reactions += {
          case ButtonClicked(_) =>
            val sp = new AreaLightProvider
            detailsTable.model = sp
            worldProvider.lightDescriptionProvider += sp
            updateUI( Some( sp ) )
        }
      }
      c.fill = Fill.Horizontal
      c.weightx = 0.5
      c.gridx = 3
      c.gridy = 1
      layout( newAreaLightButton ) = c
    }
    c.fill = Fill.Horizontal
    c.weightx = 0.5
    c.gridx = 0
    c.gridy = 1
    c.gridwidth = 6
    layout( lights ) = c



    val cameraPopupMenu = new JPopupMenu
    val createOrthographicCameraMenuItem = new JMenuItem( "Orthographic Camera" )
    createOrthographicCameraMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val ocp = new OrthograpicCameraProvider
        worldProvider.cameraProvider = Some( ocp )
        detailsTable.model = ocp
        updateUI( Some( ocp ))
      }
    })
    val createPerspectiveCameraMenuItem = new JMenuItem( "Perspective Camera" )
    createPerspectiveCameraMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val pcp = new PerspectiveCameraProvider
        worldProvider.cameraProvider = Some( pcp )
        detailsTable.model = pcp
        updateUI( Some( pcp ))
      }
    })
    val createDOFCameraMenuItem = new JMenuItem( "DOF Camera" )
    createDOFCameraMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val dofcp = new DOFCameraProvider
        worldProvider.cameraProvider = Some( dofcp )
        detailsTable.model = dofcp
        updateUI( Some( dofcp ))
      }
    })

    cameraPopupMenu.add( createOrthographicCameraMenuItem )
    cameraPopupMenu.add( createPerspectiveCameraMenuItem )
    cameraPopupMenu.add( createDOFCameraMenuItem )

    var selectionParent : Option[AnyRef] = None
    var selection : Option[AnyRef] = None

    val samplingPatternPopupMenu = new JPopupMenu
    val regularSamplingPatternMenuItem = new JMenuItem( "Regular sampling pattern" )
    regularSamplingPatternMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val rspp = new RegularSamplingPatternProvider
        selectionParent match {
          case Some( ocp : OrthograpicCameraProvider ) =>
            ocp.samplingPatternProvider = Some( rspp )
          case Some( pcp : PerspectiveCameraProvider ) =>
            pcp.samplingPatternProvider = Some( rspp )
          case Some( dcp : DOFCameraProvider ) =>
            selection match {
              case Some( "<Anti-Aliasing Sampling Pattern>" ) =>
                dcp.aaSamplingPatternProvider = Some( rspp )
              case Some( "<Lens Sampling Pattern>" ) =>
                dcp.lensSamplingPatternProvider = Some( rspp )
              case Some( spp : SamplingPatternProvider ) =>
                if( dcp.aaSamplingPatternProvider.isDefined && dcp.aaSamplingPatternProvider.get == spp ) dcp.aaSamplingPatternProvider = Some( rspp )
                if( dcp.lensSamplingPatternProvider.isDefined && dcp.lensSamplingPatternProvider.get == spp ) dcp.lensSamplingPatternProvider = Some( rspp )
              case Some( _ ) => assert( false, "This should not happen!")
              case None =>
            }
          case Some( _ ) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = rspp
        updateUI( Some( rspp ) )
      }
    })

    samplingPatternPopupMenu.add( regularSamplingPatternMenuItem )

    val materialPopupMenu = new JPopupMenu
    val newSingleColorMaterialMenuItem = new JMenuItem( "Single color material" )
    newSingleColorMaterialMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val scmp =  new SingleColorMaterialProvider
        selectionParent match {
          case Some( pp : PlaneProvider ) =>
            pp.materialProvider = Some( scmp )
          case Some( sp : SphereProvider ) =>
            sp.materialProvider = Some( scmp)
          case Some( bp : AxisAlignedBoxProvider ) =>
            bp.materialProvider = Some( scmp )
          case Some( tp : TriangleProvider ) =>
            tp.materialProvider = Some( scmp )
          case Some( mp : ModelProvider ) =>
            mp.materialProvider = Some( scmp )
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = scmp
        updateUI( Some( scmp ) )
      }
    })

    val newLambertMaterialMenuItem = new JMenuItem( "Lambert material" )
    newLambertMaterialMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val lmp = new LambertMaterialProvider
        selectionParent match {
          case Some( pp : PlaneProvider ) =>
            pp.materialProvider = Some( lmp )
          case Some( sp : SphereProvider ) =>
            sp.materialProvider = Some( lmp )
          case Some( bp : AxisAlignedBoxProvider ) =>
            bp.materialProvider = Some( lmp )
          case Some( tp : TriangleProvider ) =>
            tp.materialProvider = Some( lmp )
          case Some( mp : ModelProvider ) =>
            mp.materialProvider = Some( lmp )
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = lmp
        updateUI( Some( lmp ) )
      }
    })

    val newPhongMaterialMenuItem = new JMenuItem( "Phong material" )
    newPhongMaterialMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val pmp = new PhongMaterialProvider
        selectionParent match {
          case Some( pp : PlaneProvider ) =>
            pp.materialProvider = Some( pmp )
          case Some( sp : SphereProvider ) =>
            sp.materialProvider = Some( pmp )
          case Some( bp : AxisAlignedBoxProvider ) =>
            bp.materialProvider = Some( pmp )
          case Some( tp : TriangleProvider ) =>
            tp.materialProvider = Some( pmp )
          case Some( mp : ModelProvider ) =>
            mp.materialProvider = Some( pmp )
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = pmp
        updateUI( Some( pmp ) )
      }
    })

    val newReflectiveMaterialMenuItem = new JMenuItem( "Reflective material" )
    newReflectiveMaterialMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val rmp = new ReflectiveMaterialProvider
        selectionParent match {
          case Some( pp : PlaneProvider ) =>
            pp.materialProvider = Some( rmp )
          case Some( sp : SphereProvider ) =>
            sp.materialProvider = Some( rmp )
          case Some( bp : AxisAlignedBoxProvider ) =>
            bp.materialProvider = Some( rmp )
          case Some( tp : TriangleProvider ) =>
            tp.materialProvider = Some( rmp )
          case Some( mp : ModelProvider ) =>
            mp.materialProvider = Some( rmp )
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = rmp
        updateUI( Some( rmp ) )
      }
    })

    val newTransparentMaterialMenuItem = new JMenuItem( "Transparent material" )
    newTransparentMaterialMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val tmp = new TransparentMaterialProvider
        selectionParent match {
          case Some( pp : PlaneProvider ) =>
            pp.materialProvider = Some( tmp )
          case Some( sp : SphereProvider ) =>
            sp.materialProvider = Some( tmp )
          case Some( bp : AxisAlignedBoxProvider ) =>
            bp.materialProvider = Some( tmp )
          case Some( tp : TriangleProvider ) =>
            tp.materialProvider = Some( tmp )
          case Some( mp : ModelProvider ) =>
            mp.materialProvider = Some( tmp )
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = tmp
        updateUI( Some( tmp ) )
      }
    })

    materialPopupMenu.add( newSingleColorMaterialMenuItem )
    materialPopupMenu.add( newLambertMaterialMenuItem )
    materialPopupMenu.add( newPhongMaterialMenuItem )
    materialPopupMenu.add( newReflectiveMaterialMenuItem )
    materialPopupMenu.add( newTransparentMaterialMenuItem )

    val newTexturePopupMenu = new JPopupMenu
    val newSingleColorTextureMenuItem = new JMenuItem( "Single color texture" )
    newSingleColorTextureMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val sctp = new SingleColorTextureProvider
        selectionParent match {
          case Some( lp : LambertMaterialProvider ) =>
            lp.diffuseTextureProvider = Some( sctp )
          case Some( pp : PhongMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                pp.diffuseTextureProvider = Some( sctp )
              case Some( "<Specular Texture>" ) =>
                pp.specularTextureProvider = Some( sctp )
              case Some( tp : TextureProvider ) =>
                if( pp.diffuseTextureProvider.isDefined && pp.diffuseTextureProvider.get == tp ) pp.diffuseTextureProvider = Some( sctp )
                if( pp.specularTextureProvider.isDefined && pp.specularTextureProvider.get == tp ) pp.specularTextureProvider = Some( sctp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some( rp : ReflectiveMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                rp.diffuseTextureProvider = Some( sctp )
              case Some( "<Specular Texture>" ) =>
                rp.specularTextureProvider = Some( sctp )
              case Some( "<Reflection Texture>" ) =>
                rp.reflectionTextureProvider = Some( sctp )
              case Some( tp : TextureProvider ) =>
                if( rp.diffuseTextureProvider.isDefined && rp.diffuseTextureProvider.get == tp ) rp.diffuseTextureProvider = Some( sctp )
                if( rp.specularTextureProvider.isDefined && rp.specularTextureProvider.get == tp ) rp.specularTextureProvider = Some( sctp )
                if( rp.reflectionTextureProvider.isDefined && rp.reflectionTextureProvider.get == tp ) rp.reflectionTextureProvider = Some( sctp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = sctp
        updateUI( Some( sctp ) )
      }
    })

    val newChessboardTextureMenuItem = new JMenuItem( "Chessboard texture" )
    newChessboardTextureMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val ctp = new ChessboardTextureProvider
        selectionParent match {
          case Some( lp : LambertMaterialProvider ) =>
            lp.diffuseTextureProvider = Some( ctp )
          case Some( pp : PhongMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                pp.diffuseTextureProvider = Some( ctp )
              case Some( "<Specular Texture>" ) =>
                pp.specularTextureProvider = Some( ctp )
              case Some( tp : TextureProvider ) =>
                if( pp.diffuseTextureProvider.isDefined && pp.diffuseTextureProvider.get == tp ) pp.diffuseTextureProvider = Some( ctp )
                if( pp.specularTextureProvider.isDefined && pp.specularTextureProvider.get == tp ) pp.specularTextureProvider = Some( ctp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some( rp : ReflectiveMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                rp.diffuseTextureProvider = Some( ctp )
              case Some( "<Specular Texture>" ) =>
                rp.specularTextureProvider = Some( ctp )
              case Some( "<Reflection Texture>" ) =>
                rp.reflectionTextureProvider = Some( ctp )
              case Some( tp : TextureProvider ) =>
                if( rp.diffuseTextureProvider.isDefined && rp.diffuseTextureProvider.get == tp ) rp.diffuseTextureProvider = Some( ctp )
                if( rp.specularTextureProvider.isDefined && rp.specularTextureProvider.get == tp ) rp.specularTextureProvider = Some( ctp )
                if( rp.reflectionTextureProvider.isDefined && rp.reflectionTextureProvider.get == tp ) rp.reflectionTextureProvider = Some( ctp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = ctp
        updateUI( Some( ctp ) )
      }
    })

    val newImageTextureMenuItem = new JMenuItem( "Image texture" )
    newImageTextureMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val itp = new ImageTextureProvider( ui )
        selectionParent match {
          case Some( lp : LambertMaterialProvider ) =>
            lp.diffuseTextureProvider = Some( itp )
          case Some( pp : PhongMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                pp.diffuseTextureProvider = Some( itp )
              case Some( "<Specular Texture>" ) =>
                pp.specularTextureProvider = Some( itp )
              case Some( tp : TextureProvider ) =>
                if( pp.diffuseTextureProvider.isDefined && pp.diffuseTextureProvider.get == tp ) pp.diffuseTextureProvider = Some( itp )
                if( pp.specularTextureProvider.isDefined && pp.specularTextureProvider.get == tp ) pp.specularTextureProvider = Some( itp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some( rp : ReflectiveMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                rp.diffuseTextureProvider = Some( itp )
              case Some( "<Specular Texture>" ) =>
                rp.specularTextureProvider = Some( itp )
              case Some( "<Reflection Texture>" ) =>
                rp.reflectionTextureProvider = Some( itp )
              case Some( tp : TextureProvider ) =>
                if( rp.diffuseTextureProvider.isDefined && rp.diffuseTextureProvider.get == tp ) rp.diffuseTextureProvider = Some( itp )
                if( rp.specularTextureProvider.isDefined && rp.specularTextureProvider.get == tp ) rp.specularTextureProvider = Some( itp )
                if( rp.reflectionTextureProvider.isDefined && rp.reflectionTextureProvider.get == tp ) rp.reflectionTextureProvider = Some( itp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = itp
        updateUI( Some( itp ) )
      }
    })

    val newInterpolatedImageTextureMenuItem = new JMenuItem( "Interpolated image texture" )
    newInterpolatedImageTextureMenuItem.addActionListener( new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val iitp = new InterpolatedImageTextureProvider( ui )
        selectionParent match {
          case Some( lp : LambertMaterialProvider ) =>
            lp.diffuseTextureProvider = Some( iitp )
          case Some( pp : PhongMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                pp.diffuseTextureProvider = Some( iitp )
              case Some( "<Specular Texture>" ) =>
                pp.specularTextureProvider = Some( iitp )
              case Some( tp : TextureProvider ) =>
                if( pp.diffuseTextureProvider.isDefined && pp.diffuseTextureProvider.get == tp ) pp.diffuseTextureProvider = Some( iitp )
                if( pp.specularTextureProvider.isDefined && pp.specularTextureProvider.get == tp ) pp.specularTextureProvider = Some( iitp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some( rp : ReflectiveMaterialProvider ) =>
            selection match {
              case Some( "<Diffuse Texture>" ) =>
                rp.diffuseTextureProvider = Some( iitp )
              case Some( "<Specular Texture>" ) =>
                rp.specularTextureProvider = Some( iitp )
              case Some( "<Reflection Texture>" ) =>
                rp.reflectionTextureProvider = Some( iitp)
              case Some( tp : TextureProvider ) =>
                if( rp.diffuseTextureProvider.isDefined && rp.diffuseTextureProvider.get == tp ) rp.diffuseTextureProvider = Some( iitp )
                if( rp.specularTextureProvider.isDefined && rp.specularTextureProvider.get == tp ) rp.specularTextureProvider = Some( iitp )
                if( rp.reflectionTextureProvider.isDefined && rp.reflectionTextureProvider.get == tp ) rp.reflectionTextureProvider = Some( iitp )
              case Some(_) => assert( false, "This should not happen!")
              case None =>
            }
          case Some(_) => assert( false, "This should not happen!")
          case None =>
        }
        detailsTable.model = iitp
        updateUI( Some( iitp ) )
      }
    })

    newTexturePopupMenu.add( newSingleColorTextureMenuItem )
    newTexturePopupMenu.add( newChessboardTextureMenuItem )
    newTexturePopupMenu.add( newImageTextureMenuItem )
    newTexturePopupMenu.add( newInterpolatedImageTextureMenuItem )


    val sceneGraphTree = new JTree
    sceneGraphTree.setExpandsSelectedPaths( true )
    sceneGraphTree.setModel( new SceneGraphTreeModel( worldProvider ) )
    sceneGraphTree.addKeyListener( new KeyListener {
      def keyTyped(e: KeyEvent) {
        e.getKeyChar match {
          case KeyEvent.VK_DELETE =>
            val node = sceneGraphTree.getLastSelectedPathComponent
            if( node != null ) {
              val s = sceneGraphTree.getSelectionRows
              worldProvider.remove( node )
              updateUI( None )
              detailsTable.model = DummyTableModel
              sceneGraphTree.setSelectionRows( s )
            }
          case _ =>
        }
      }

      def keyPressed(e: KeyEvent) {}

      def keyReleased(e: KeyEvent) {}
    } )

    sceneGraphTree.addMouseListener( new MouseAdapter {
      override def mousePressed(e: MouseEvent) {
        val path = sceneGraphTree.getPathForLocation( e.getX, e.getY )

        if( path != null && SwingUtilities.isRightMouseButton( e ) ) {
          path.getLastPathComponent match {
            case "<Camera>" =>
              cameraPopupMenu.show( e.getComponent, e.getX, e.getY )

            case cp : CameraProvider =>
              cameraPopupMenu.show( e.getComponent, e.getX, e.getY )

            case "<Anti-Aliasing Sampling Pattern>" =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              samplingPatternPopupMenu.show( e.getComponent, e.getX, e.getY )

            case "<Lens Sampling Pattern>" =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              samplingPatternPopupMenu.show( e.getComponent, e.getX, e.getY )

            case spp : SamplingPatternProvider =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              samplingPatternPopupMenu.show( e.getComponent, e.getX, e.getY )

            case "<Material>" =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              materialPopupMenu.show( e.getComponent, e.getX, e.getY )

            case mp : MaterialProvider =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              materialPopupMenu.show( e.getComponent, e.getX, e.getY )

            case "<Diffuse Texture>" =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              newTexturePopupMenu.show( e.getComponent, e.getX, e.getY )

            case "<Specular Texture>" =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              newTexturePopupMenu.show( e.getComponent, e.getX, e.getY )

            case "<Reflection Texture>" =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              newTexturePopupMenu.show( e.getComponent, e.getX, e.getY )


            case tp : TextureProvider =>
              selection = Some( path.getLastPathComponent )
              selectionParent = Some( path.getPathComponent( path.getPathCount - 2 ) )
              newTexturePopupMenu.show( e.getComponent, e.getX, e.getY )


            case _ =>
          }

        }
      }

    })

    sceneGraphTree.addTreeSelectionListener( new TreeSelectionListener {
      def valueChanged(e: TreeSelectionEvent) {
        val node = sceneGraphTree.getLastSelectedPathComponent
        if( node != null ) {
          node match {
            case o : TableModel =>
              detailsTable.model = o
            case _ =>
              detailsTable.model = DummyTableModel
          }
        } else {
          detailsTable.model = DummyTableModel
        }
      }
    })
    sceneGraphTree.getSelectionModel.setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION )
    val sceneGraphTreeScrollPane = new ScrollPane {
      contents = new Component {
        override lazy val peer = sceneGraphTree
      }
    }

    c.fill = Fill.Both
    c.ipady = 300
    c.weightx = 0.0
    c.weighty = 0.5
    c.gridwidth = 6
    c.gridx = 0
    c.gridy = 2
    c.anchor = Anchor.North
    layout( sceneGraphTreeScrollPane ) = c

    val colorRenderer = new Table.AbstractRenderer[Color, Label](new Label) {
      def configure(t: Table, sel: Boolean, foc: Boolean, o: Color, row: Int, col: Int) = {
        component.background = new scala.swing.Color( o.rgbInteger )
        if( (o.r + o.g + o.b)/3 < 0.5 ) component.foreground = new scala.swing.Color( 255, 255, 255 )
        component.text = "(%1.2f/ %1.2f/ %1.2f)" format( o.r, o.g, o.b )
      }
    }

    val detailsTable = new Table() {

      val colorEditor = new AbstractCellEditor with TableCellEditor {
        val button = new Button( "Editing" )
        var c = Color( 0, 0, 0 )
        reactions += {
          case e: ButtonClicked => {
            ColorChooser.showDialog( button, "Choose Color", new scala.swing.Color( c.rgbInteger ) ) match {
              case Some( newColor ) =>
                c = Color( newColor.getRed().asInstanceOf[Double] / 255.0, newColor.getGreen().asInstanceOf[Double] / 255.0, newColor.getBlue().asInstanceOf[Double] / 255.0 )
              case None =>
            }
            stopCellEditing()
          }
        }
        listenTo(button)


        override def getCellEditorValue() = c

        override def getTableCellEditorComponent(table: JTable, value: AnyRef, isSelected: Boolean, row: Int, column: Int) = {
          c = value.asInstanceOf[Color]
          button.background = new scala.swing.Color( c.rgbInteger )
          if( (c.r + c.g + c.b)/3 < 0.5 ) button.foreground = new scala.swing.Color( 255, 255, 255 )
          button.peer
        }
      }

      override def rendererComponent( sel: Boolean, foc: Boolean, row: Int, col: Int ) = {
        val v = model.getValueAt( peer.convertRowIndexToModel( row ), peer.convertColumnIndexToModel( col ) )
        v match {
          case c : Color => colorRenderer.componentFor(this, sel, foc, c, row, col)
          case _ => super.rendererComponent( sel, foc, row, col )
        }
      }

      override def editor(row: Int, column: Int) = {
        val v = model.getValueAt( peer.convertRowIndexToModel( row ), peer.convertColumnIndexToModel( column ) )
        v match {
          case c : Color => colorEditor
          case _ => super.editor( row, column )
        }
      }


    }

    val detailsTableScrollPane = new ScrollPane {
      contents = detailsTable
    }



    detailsTable.model = DummyTableModel
    c.fill = Fill.Both
    c.ipady = 100
    c.weighty = 0.5
    c.anchor = Anchor.PageEnd
    c.gridwidth = 6
    c.gridx = 0
    c.gridy = 3
    layout( detailsTableScrollPane ) = c

    val renderButton = new Button {
      text = "Render"
      reactions += {
        case ButtonClicked(_) =>

          text = "Preparing"
          enabled = false
          progressBar.value = 0
          async {
            val totalSteps = worldProvider.count.asInstanceOf[Double]
            var steps = 0.0
            val (c,w) = worldProvider.createWorld( () => {
              steps = steps + 1
              progressBar.value = (steps * 100.0 / totalSteps).asInstanceOf[Int]
            })
            val window = new NiceRenderingWindow( w, c, renderingWindowsSize, Runtime.getRuntime.availableProcessors(), recursionDepth, clusterNodes.toList )
            window.a ! StartRendering()
            text = "Render"
            enabled = true
          }
      }
    }

    c.fill = Fill.Horizontal
    c.ipady = 0
    c.weighty = 0
    c.weightx = 0.5
    c.gridx = 0
    c.gridy = 4
    layout( renderButton ) = c

    val progressBar = new ProgressBar{
      min = 0
      max = 100
      value = 0
    }

    c.fill = Fill.Horizontal
    c.ipady = 0
    c.weighty = 0
    c.weightx = 0.5
    c.gridx = 0
    c.gridy = 5
    layout( progressBar ) = c

    private def createPath( root : AnyRef, item : AnyRef, model : TreeModel, path : Vector[AnyRef] ) : Option[Vector[AnyRef]] = {
      if( root == item )
        Some( path :+ item )
      else {
        for( i <- 0 until model.getChildCount( root ) ) {
          val newRoot = model.getChild( root, i )
          val r = createPath( newRoot, item, model, path :+ root )
          if( r.isDefined ) return r
          }
        None
        }
      }



    private def updateUI( newElement : Option[AnyRef] ) {
      sceneGraphTree.updateUI()
      renderButton.enabled = worldProvider.isReady
      if( newElement.isDefined ) {
        val model = sceneGraphTree.getModel
        val root = sceneGraphTree.getModel.getRoot
        val path = createPath( root, newElement.get, model, Vector[AnyRef]() )
        if( path.isDefined )
          sceneGraphTree.setSelectionPath( new TreePath( path.get.toArray  ))
      }
    }

  }

  def top = new MainFrame {
    title = "ScalaDelRay"
    contents = ui
    size = new Dimension( 500, 700 )
    resizable = false
    menuBar = new MenuBar
    val fileMenu = new Menu( "FILE" )
    fileMenu.contents += new MenuItem( "New")
    fileMenu.contents += new MenuItem( "Load...")
    val renderingSettingsMenu = new Menu( "RENDERING" )
    renderingSettingsMenu.contents += new MenuItem( Action("Settings...") {

      val frame = new Frame {

        lazy val imagePage = new GridBagPanel {

          val c = new Constraints



          val widthLabel = new Label( "Width:")

          c.fill = Fill.Horizontal

          c.weightx = 0.3
          c.gridx = 0
          c.gridy = 0

          layout( widthLabel ) = c

          val widthTextField = new TextField( "" + renderingWindowsSize.getWidth.toInt )

          c.weightx = 0.7
          c.gridx = 1
          layout( widthTextField ) = c

          val heightLabel = new Label( "Height:" )

          c.weightx = 0.3
          c.gridy = 1
          c.gridx = 0
          layout( heightLabel ) = c

          val heightTextField = new TextField( "" + renderingWindowsSize.getHeight.toInt )
          c.weightx = 0.7
          c.gridx = 1
          layout( heightTextField ) = c

          // Just to correct the layout
          val dummyLabel = new Label( "" )
          c.fill = Fill.Horizontal
          c.ipady = 0
          c.weighty = 1.0
          c.anchor = Anchor.PageEnd
          c.gridx = 1
          c.gridwidth = 1
          c.gridy = 2
          layout(dummyLabel) = c

        }

        lazy val algorithmPage =  new GridBagPanel {

          val c = new Constraints

          val algorithmLabel = new Label( "Algorithm: " )

          c.fill = Fill.Horizontal

          c.weightx = 0.3
          c.gridx = 0
          c.gridy = 0

          layout( algorithmLabel ) = c

          val algorithmComboBox = new ComboBox[String]( List( "Ray Casting", "Recursive Ray Tracing", "Stochastic Ray Tracing", "Path Tracing", "Photon Mapping" ) )
          algorithmComboBox.enabled = false
          algorithmComboBox.selection.item = "Stochastic Ray Tracing"
          c.weightx = 0.7
          c.gridx = 1

          layout( algorithmComboBox ) = c

          val recursionsLabel = new Label( "Recursions: " )

          c.fill = Fill.Horizontal

          c.weightx = 0.3
          c.gridx = 0
          c.gridy = 1

          layout( recursionsLabel ) = c

          val recursionsTextField = new TextField( "" + recursionDepth )

          c.weightx = 0.7
          c.gridx = 1

          layout( recursionsTextField ) = c

          val epsilonLabel = new Label( "Epsilon: " )

          c.fill = Fill.Horizontal

          c.weightx = 0.3
          c.gridx = 0
          c.gridy = 2

          layout( epsilonLabel ) = c

          val epsilonTextField = new TextField( "" + Constants.EPSILON )
          epsilonTextField.editable = false
          c.weightx = 0.7
          c.gridx = 1

          layout( epsilonTextField ) = c

          // Just to correct the layout
          val dummyLabel = new Label( "" )
          c.fill = Fill.Horizontal
          c.ipady = 0
          c.weighty = 1.0
          c.anchor = Anchor.PageEnd
          c.gridx = 1
          c.gridwidth = 1
          c.gridy = 3
          layout(dummyLabel) = c

        }

        lazy val clusterPage =  new GridBagPanel {

          var tempClusterNodes = clusterNodes.clone()

          val c = new Constraints

          val addButton = new Button("+" )
          addButton.action = Action( "+" ) {
            tempClusterNodes += (("",4444,1))
            nodesTable.revalidate()
          }

          c.fill = Fill.Horizontal

          c.weightx = 0.5
          c.gridx = 0
          c.gridy = 0

          layout( addButton ) = c

          val deleteButton = new Button("-" )
          deleteButton.action = Action( "-" ) {
            val x = nodesTable.selection.rows.toList.reverse
            for( i <- x ) if( i < tempClusterNodes.size ) tempClusterNodes.remove( i )
            nodesTable.revalidate()
          }

          c.fill = Fill.Horizontal

          c.weightx = 0.5
          c.gridx = 1
          c.gridy = 0

          layout( deleteButton ) = c

          val nodesTable = new Table()
          val nodesTableScrollPane = new ScrollPane {
            contents = nodesTable
          }

          nodesTable.model = new TableModel {
            def getRowCount: Int = tempClusterNodes.size

            def getColumnCount: Int = 3

            def getColumnName(columnIndex: Int): String =
              columnIndex match {
                case 0 =>
                  "Address"
                case 1 =>
                  "Port"
                case 2 =>
                  "Cores"
              }

            def getColumnClass(columnIndex: Int): Class[_] =
              columnIndex match {
                case 0 =>
                  classOf[String]
                case 1 =>
                  classOf[java.lang.Integer]
                case 2 =>
                  classOf[java.lang.Integer]
              }


            def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

            def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
              val (address,port,cores) = tempClusterNodes( rowIndex )
              columnIndex match {
                case 0 =>
                  address
                case 1 =>
                  port.asInstanceOf[java.lang.Integer]
                case 2 =>
                  cores.asInstanceOf[java.lang.Integer]
              }
            }

            def setValueAt(aValue: scala.Any, rowIndex: Int, columnIndex: Int) {
              val (address,port,cores) = tempClusterNodes( rowIndex )
              columnIndex match {
                case 0 =>
                  tempClusterNodes.update( rowIndex, (aValue.asInstanceOf[String],port,cores) )
                case 1 =>
                  tempClusterNodes.update( rowIndex, (address,aValue.asInstanceOf[Int],cores) )
                case 2 =>
                  tempClusterNodes.update( rowIndex, (address,port,aValue.asInstanceOf[Int]) )

              }
            }

            def addTableModelListener(l: TableModelListener) {}

            def removeTableModelListener(l: TableModelListener) {}
          }
          c.fill = Fill.Both
          c.ipady = 0
          c.weighty = 0.5
          c.anchor = Anchor.PageEnd
          c.gridwidth = 2
          c.gridx = 0
          c.gridy = 1
          layout( nodesTableScrollPane ) = c



          val discoverButton = new Button("Discover" )
          discoverButton.action = Action( "Discover" ) {
            discoverButton.enabled = false
            okButton.enabled = false
            cancelButton.enabled = false
            addButton.enabled = false
            deleteButton.enabled = false

            val socket = new DatagramSocket()
            val buf = new Array[Byte]( 256 )
            val address = InetAddress.getByName("228.5.6.7")
            var packet = new DatagramPacket(buf, buf.length, address, 12345 )
            packet.setData( "ScalaDelRay:1.0".getBytes )
            socket.send( packet )

            val discoveryActor = actorSystem.actorOf( Props( new Actor {

              def receive = {
                case m : StartDiscovery =>
                  try {
                    while( true ) {
                      packet = new DatagramPacket(buf, buf.length, address, 12345 )
                      socket.receive( packet )
                      val nodeAddress = new String( packet.getData, 8, packet.getLength - 8 )
                      val nodePort = (packet.getData()(0) << 24) | (packet.getData()(1) << 16) |  (packet.getData()(2) << 8) |  packet.getData()(3)
                      val nodeCores = (packet.getData()(4) << 24) | (packet.getData()(5) << 16) |  (packet.getData()(6) << 8) |  packet.getData()(7)

                      if( !tempClusterNodes.contains( (nodeAddress,nodePort,nodeCores) ) ) tempClusterNodes += ((nodeAddress,nodePort,nodeCores))

                      nodesTable.revalidate()
                    }
                  } catch {
                    case ex : SocketException =>

                  }
                  self ! PoisonPill

              }

            }))

            discoveryActor ! StartDiscovery()

            actorSystem.actorOf( Props( new Actor {

              discoverProgressBar.value = 0
              this.context.setReceiveTimeout( 0.0625 second )

              def receive = {
                case m : ReceiveTimeout =>
                discoverProgressBar.value = discoverProgressBar.value + 1

                if( discoverProgressBar.value == 60 ) {
                  self ! PoisonPill
                  discoverButton.enabled = true
                  okButton.enabled = true
                  cancelButton.enabled = true
                  addButton.enabled = true
                  deleteButton.enabled = true
                  socket.close()

                }

              }

            }))



          }

          c.ipady = 0
          c.weightx = 0.5
          c.weighty = 0
          c.gridx = 0
          c.gridy = 2
          c.gridwidth = 2


          layout( discoverButton ) = c

          val discoverProgressBar = new ProgressBar() {
            min = 0
            max = 60
          }


          c.gridy = 3


          layout( discoverProgressBar ) = c



        }

        title = "Rendering settings"

        val cancelButton = new Button("Cancel" )
        val okButton = new Button("Ok" )

        contents = new GridBagPanel {
          val c = new Constraints

          c.fill = Fill.Both
          c.weightx = 0.5
          c.weighty = 0.5
          c.gridx = 0
          c.gridy = 0
          c.gridwidth = 2


          c.anchor = Anchor.PageEnd

          val tabbedPane = new TabbedPane {
            pages += new Page( "Image", imagePage )
            pages += new Page( "Algorithm", algorithmPage )
            pages += new Page( "Cluster", clusterPage )
          }

          layout( tabbedPane ) = c
          c.weighty = 0.5
          c.weighty = 0
          c.fill = Fill.Horizontal
          c.gridwidth = 1


          cancelButton.action = Action( "Cancel" ) {
            close()
          }
          c.gridy = 1
          c.gridx = 0
          layout( cancelButton ) = c


          okButton.action = Action( "Ok" ) {
            renderingWindowsSize = new Dimension( imagePage.widthTextField.text.toInt, imagePage.heightTextField.text.toInt )
            recursionDepth = algorithmPage.recursionsTextField.text.toInt
            clusterNodes = clusterPage.tempClusterNodes
            close()
          }
          c.gridx = 1
          layout( okButton ) = c


        }
        visible = true


      }
    })
    val helpMenu = new Menu( "HELP" )
    helpMenu.contents += new MenuItem( "About" )
    //menuBar.contents += fileMenu
    menuBar.contents += renderingSettingsMenu
    menuBar.contents += helpMenu



  }

  private def createStandardScene : WorldProvider = {
    val worldProvider = new WorldProvider
    worldProvider.ambientLight = Color( 0.1, 0.1, 0.1 )
    worldProvider.backgroundColor = Color( 0.1, 0.1, 0.1 )

    val cameraProvider = new PerspectiveCameraProvider
    cameraProvider.position = Point3( -1.52, 1.49, 4.37 )
    cameraProvider.gazeDirection = Vector3( 0, 0, -1 )
    cameraProvider.samplingPatternProvider.get.asInstanceOf[RegularSamplingPatternProvider].x = 5
    cameraProvider.samplingPatternProvider.get.asInstanceOf[RegularSamplingPatternProvider].y = 5
    cameraProvider.angle = 60.0 / 180.0 * Math.PI
    worldProvider.cameraProvider = Some( cameraProvider )

    val bottomPlaneProvider = new PlaneProvider
    val bottomPlaneLambertMaterialProvider = new LambertMaterialProvider
    val bottomPlaneDiffuseTextureProvider = new SingleColorTextureProvider
    bottomPlaneDiffuseTextureProvider.color = Color( 1, 1, 1 )
    bottomPlaneLambertMaterialProvider.diffuseTextureProvider = Some( bottomPlaneDiffuseTextureProvider )
    bottomPlaneProvider.materialProvider = Some( bottomPlaneLambertMaterialProvider )
    worldProvider.geometryProvider += bottomPlaneProvider

    val topPlaneProvider = new PlaneProvider
    topPlaneProvider.translate = Point3( 0, 3, 0 )
    topPlaneProvider.rotate = Vector3( Math.PI, 0, 0 )
    val topPlaneLambertMaterialProvider = new LambertMaterialProvider
    val topPlaneDiffuseTextureProvider = new SingleColorTextureProvider
    topPlaneDiffuseTextureProvider.color = Color( 1, 1, 1 )
    topPlaneLambertMaterialProvider.diffuseTextureProvider = Some( topPlaneDiffuseTextureProvider )
    topPlaneProvider.materialProvider = Some( topPlaneLambertMaterialProvider )
    worldProvider.geometryProvider += topPlaneProvider

    val leftPlaneProvider = new PlaneProvider
    leftPlaneProvider.translate = Point3( -3.04, 0, 0 )
    leftPlaneProvider.rotate = Vector3( 0, 0, -Math.PI / 2.0 )
    val leftPlaneLambertMaterialProvider = new LambertMaterialProvider
    val leftPlaneDiffuseTextureProvider = new SingleColorTextureProvider
    leftPlaneDiffuseTextureProvider.color = Color( 1, 0, 0 )
    leftPlaneLambertMaterialProvider.diffuseTextureProvider = Some( leftPlaneDiffuseTextureProvider )
    leftPlaneProvider.materialProvider = Some( leftPlaneLambertMaterialProvider )
    worldProvider.geometryProvider += leftPlaneProvider

    val rightPlaneProvider = new PlaneProvider
    rightPlaneProvider.translate = Point3( 0, 0, 0 )
    rightPlaneProvider.rotate = Vector3( 0, 0, Math.PI / 2.0 )
    val rightPlaneLambertMaterialProvider = new LambertMaterialProvider
    val rightPlaneDiffuseTextureProvider = new SingleColorTextureProvider
    rightPlaneDiffuseTextureProvider.color = Color( 0, 1, 0 )
    rightPlaneLambertMaterialProvider.diffuseTextureProvider = Some( rightPlaneDiffuseTextureProvider )
    rightPlaneProvider.materialProvider = Some( rightPlaneLambertMaterialProvider )
    worldProvider.geometryProvider += rightPlaneProvider

    val backPlaneProvider = new PlaneProvider
    backPlaneProvider.translate = Point3( 0, 0, -3.06 )
    backPlaneProvider.rotate = Vector3( Math.PI / 2.0, 0, 0 )
    val backPlaneLambertMaterialProvider = new LambertMaterialProvider
    val backPlaneDiffuseTextureProvider = new SingleColorTextureProvider
    backPlaneDiffuseTextureProvider.color = Color( 1, 1, 1 )
    backPlaneLambertMaterialProvider.diffuseTextureProvider = Some( backPlaneDiffuseTextureProvider )
    backPlaneProvider.materialProvider = Some( backPlaneLambertMaterialProvider )
    worldProvider.geometryProvider += backPlaneProvider

    val shortBoxProvider = new AxisAlignedBoxProvider
    shortBoxProvider.scale = Vector3( 0.92, 0.9, 1.28 )
    shortBoxProvider.rotate = Vector3( 0, -22.0 / 180.0 * Math.PI, 0 )
    shortBoxProvider.translate = Point3( -1.025, 0.45, -0.925 )
    val shortBoxLambertMaterialProvider = new LambertMaterialProvider
    val shortBoxDiffuseTextureProvider = new SingleColorTextureProvider
    shortBoxDiffuseTextureProvider.color = Color( 0.5, 0.5, 0.5 )
    shortBoxLambertMaterialProvider.diffuseTextureProvider = Some( shortBoxDiffuseTextureProvider )
    shortBoxProvider.materialProvider = Some( shortBoxLambertMaterialProvider )
    worldProvider.geometryProvider += shortBoxProvider

    val tallBoxProvider = new AxisAlignedBoxProvider
    tallBoxProvider.scale = Vector3( 0.92, 1.80, 1.28 )
    tallBoxProvider.rotate = Vector3( 0, 17.24 / 180.0 * Math.PI, 0 )
    tallBoxProvider.translate = Point3( -2.0125, 0.9, -1.8825 )
    val tallBoxLambertMaterialProvider = new LambertMaterialProvider
    val tallBoxDiffuseTextureProvider = new SingleColorTextureProvider
    tallBoxDiffuseTextureProvider.color = Color( 0.5, 0.5, 0.5 )
    tallBoxLambertMaterialProvider.diffuseTextureProvider = Some( tallBoxDiffuseTextureProvider )
    tallBoxProvider.materialProvider = Some( tallBoxLambertMaterialProvider )
    worldProvider.geometryProvider += tallBoxProvider

    val areaLightProvider = new AreaLightProvider
    areaLightProvider.position = Point3( -1.52, 2.95, -1.52 )
    areaLightProvider.direction = Vector3( 0, -1, 0 )
    areaLightProvider.upVector = Vector3( 0, 0, -1 )
    areaLightProvider.size = 0.71
    areaLightProvider.samplingPoints = 16

    worldProvider.lightDescriptionProvider += areaLightProvider

    worldProvider
  }

  private def createSpheresScene : WorldProvider = {
    val worldProvider = new WorldProvider
    worldProvider.ambientLight = Color( 0.1, 0.1, 0.1 )
    worldProvider.backgroundColor = Color( 0.1, 0.1, 0.1 )
    val cameraProvider = new PerspectiveCameraProvider
    cameraProvider.position = Point3( 4, 3.0, 10.0 )
    cameraProvider.gazeDirection = Point3( 4, 3.0, 10.0 ).asVector * -1
    cameraProvider.samplingPatternProvider.get.asInstanceOf[RegularSamplingPatternProvider].x = 5
    cameraProvider.samplingPatternProvider.get.asInstanceOf[RegularSamplingPatternProvider].y = 5

    worldProvider.cameraProvider = Some( cameraProvider )

    val r = new Random

    for( x <- 0 to 10 ) {
      for( y <- 0 to 10 ) {
        for( z <- 0 to 10 ) {
          val sphereProvider = new SphereProvider
          val sphereReflectiveMaterial = new ReflectiveMaterialProvider
          sphereReflectiveMaterial.phongExponent = 64
          val sphereDiffuseColorTexture = new SingleColorTextureProvider
          sphereDiffuseColorTexture.color = Color( r.nextDouble() * 0.25, r.nextDouble() * 0.25, r.nextDouble() * 0.25 )
          val sphereSpecularColorTexture = new SingleColorTextureProvider
          sphereSpecularColorTexture.color = Color( 0.5, 0.5, 0.5 )
          val sphereMirrorColorTexture = new SingleColorTextureProvider
          sphereMirrorColorTexture.color = Color( 0.75, 0.75, 0.75 )
          sphereReflectiveMaterial.diffuseTextureProvider = Some( sphereDiffuseColorTexture )
          sphereReflectiveMaterial.specularTextureProvider = Some( sphereSpecularColorTexture )
          sphereReflectiveMaterial.reflectionTextureProvider = Some( sphereMirrorColorTexture )
          sphereProvider.materialProvider = Some( sphereReflectiveMaterial )
          sphereProvider.scale = Vector3( 0.25, 0.25, 0.25 )
          sphereProvider.translate = Point3( x - 5, y - 5, z - 5 )
          worldProvider.geometryProvider += sphereProvider
        }
      }
    }

    val pointLightProvider = new PointLightProvider
    pointLightProvider.position = Point3( 0, 0, 10 )
    pointLightProvider.color = Color( 1.5, 1.5, 1.5 )

    worldProvider.lightDescriptionProvider += pointLightProvider

    worldProvider
  }

}


object DummyTableModel extends TableModel {

  def getRowCount: Int = 0

  def getColumnCount: Int = 2

  def getColumnName( column : Int): String = column match {
    case 0 => "Property"
    case 1 => "Value"
  }

  def getColumnClass(row: Int): Class[_] = classOf[String]

  def isCellEditable(row: Int, column: Int): Boolean = false

  def getValueAt(row: Int, column: Int): AnyRef = ""

  def setValueAt(obj: Any, row: Int, column: Int) {}

  def addTableModelListener(p1: TableModelListener) {}

  def removeTableModelListener(p1: TableModelListener) {}


}


