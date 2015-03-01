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

import javax.swing.table.TableModel
import scaladelray.texture.{InterpolatedImageTexture, Texture}
import javax.swing.event.{TableModelEvent, TableModelListener}
import java.io.File
import scala.collection.mutable

class InterpolatedImageTextureProvider( tml : TableModelListener ) extends TextureProvider with TableModel {

  var fileName = ""
  var flipHorizontally = false
  var flipVertically = false

  var listener = mutable.Set[TableModelListener]()

  listener += tml

  def createTexture: Texture = InterpolatedImageTexture( fileName, flipHorizontally, flipVertically )

  def getRowCount: Int = 3

  def getColumnCount: Int = 2

  def getColumnName(column: Int): String = column match {
    case 0 => "Property"
    case 1 => "Value"
  }

  def getColumnClass(p1: Int): Class[_] = classOf[String]

  def isCellEditable(row: Int, column: Int): Boolean = column == 1

  def getValueAt(row: Int, column: Int): AnyRef = column match {
    case 0 => row match {
      case 0 => "File"
      case 1 => "Flip Horizontally"
      case 2 => "Flip Vertically"
    }
    case 1 => row match {
      case 0 => fileName
      case 1 => new java.lang.Boolean( flipHorizontally )
      case 2 => new java.lang.Boolean( flipVertically )
    }
  }

  def setValueAt( obj : Any, row: Int, column: Int) {
    row match {
      case 0 =>
        fileName = obj.asInstanceOf[String]
        for( l <- listener ) l.tableChanged( new TableModelEvent( this ) )
      case 1 =>
        flipHorizontally = obj.asInstanceOf[Boolean]
        for( l <- listener ) l.tableChanged( new TableModelEvent( this ) )
      case 2 =>
        flipVertically = obj.asInstanceOf[Boolean]
        for( l <- listener ) l.tableChanged( new TableModelEvent( this ) )
    }
  }

  def addTableModelListener( l : TableModelListener) {
    listener += l
  }

  def removeTableModelListener( l : TableModelListener) {
    listener -= l
  }

  def isReady: Boolean = new File( fileName ).exists()

  override def toString: String = "Interpolated image texture"

}
