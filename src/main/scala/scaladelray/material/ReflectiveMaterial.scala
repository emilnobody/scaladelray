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

package scaladelray.material

import scaladelray.texture.Texture
import scaladelray.geometry.Hit
import scaladelray.{Color, World}
import scaladelray.math.Ray


case class ReflectiveMaterial( diffuseTexture : Texture, specularTexture : Texture, phongExponent : Int, reflectionTexture : Texture ) extends Material {
  override def colorFor( hit: Hit, world : World, tracer : ((Ray,World) => Color) ) : Color = {
    val diffuseColor = diffuseTexture( hit.texCoord2D )
    val specularColor = specularTexture( hit.texCoord2D )
    val reflectionColor = reflectionTexture( hit.texCoord2D )

    val normal = hit.n
    val p =  hit.ray( hit.t )
    var c = world.ambientLight * diffuseColor
    val e = (hit.ray.d * -1).normalized
    for( light <- world.lights ) {
      if( light.illuminates( p, world ) ) {
        val l = light.directionFrom( p )
        val r = l.reflectOn( normal )
        c = c + (light.color * diffuseColor * math.max(0, normal dot l) ) + (light.color * specularColor * scala.math.pow( scala.math.max( 0, r dot e ), phongExponent ))
      }
    }
    val ray = Ray(p, (hit.ray.d.normalized * -1) reflectOn normal)
    c + (reflectionColor * tracer( ray, world ))
  }
}