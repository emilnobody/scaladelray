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

package scaladelray.ui.model

import scaladelray.geometry.{Node, Geometry}
import scaladelray.math.{Transform, Vector3, Point3}
import scala.collection.mutable
import javax.swing.table.TableModel
import javax.swing.event.TableModelListener

class NodeProvider extends GeometryProvider with TableModel {

  var childNodes = mutable.MutableList[GeometryProvider]()
  var translate = Point3( 0, 0, 0 )
  var scale = Vector3( 1, 1, 1 )
  var rotate = Vector3( 0, 0, 0 )

  override def createGeometry( l : () => Unit ) : Geometry = {
    l()
    val cn = for( n <- childNodes ) yield n.createGeometry( l )
    val t = Transform.translate( translate ).rotateZ( rotate.z ).rotateY(rotate.y ).rotateX( rotate.x ).scale( scale.x, scale.y, scale.z )
    new Node( t, cn:_*  )
  }

  override def remove(obj: AnyRef) {
    obj match {
      case gp : GeometryProvider =>
        if( childNodes.contains( gp ) ) childNodes = childNodes.filterNot( _ == gp ) else for( v <- childNodes ) v.remove( gp )
      case _ =>
    }
  }

  override def getRowCount: Int = 3

  override def getColumnCount: Int = 2

  override def getColumnName( column : Int): String = column match {
    case 0 => "Property"
    case 1 => "Value"
  }

  override def getColumnClass(row: Int): Class[_] = classOf[String]

  override def isCellEditable(row: Int, column: Int): Boolean = column match {
    case 0 => false
    case 1 => true
  }

  override def getValueAt(row: Int, column: Int): AnyRef = column match {
    case 0 =>
      row match {
        case 0 =>
          "Translate"
        case 1 =>
          "Scale"
        case 2 =>
          "Rotate"
      }
    case 1 =>
      row match {
        case 0 =>
          "" + translate.x + " " + translate.y + " " + translate.z
        case 1 =>
          "" + scale.x + " " + scale.y + " " + scale.z
        case 2 =>
          "" + math.toDegrees( rotate.x ) + " " + math.toDegrees( rotate.y ) + " " + math.toDegrees( rotate.z )
      }
  }

  override def setValueAt(obj: Any, row: Int, column: Int) {
    try {
      row match {
        case 0 =>
          val v = obj.asInstanceOf[String].split( " " )
          translate = Point3( v(0).toDouble, v(1).toDouble, v(2).toDouble )
        case 1 =>
          val v = obj.asInstanceOf[String].split( " " )
          scale = Vector3( v(0).toDouble, v(1).toDouble, v(2).toDouble )
        case 2 =>
          val v = obj.asInstanceOf[String].split( " " )
          rotate = Vector3( math.toRadians( v(0).toDouble ), math.toRadians( v(1).toDouble ), math.toRadians( v(2).toDouble ) )
      }
    } catch {
      case _ : Throwable =>
    }

  }

  override def addTableModelListener(p1: TableModelListener) {}

  override def removeTableModelListener(p1: TableModelListener) {}

  override def isReady: Boolean = childNodes.find( (gp) => !gp.isReady ).isEmpty

  override def toString: String = "Node"

  override def count = 1 + childNodes.foldLeft( 0 )( (v : Int, gp : GeometryProvider) => v + gp.count )
}
