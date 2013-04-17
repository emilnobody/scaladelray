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

package test.scaladelray.math

import org.scalatest.FunSpec
import scaladelray.math.{Ray, Vector3, Point3}

class RaySpec extends FunSpec {

  describe( "A Ray" ) {

    it( "should take the origin and the direction as parameter and provide them as attributes" ) {

      val o = Point3( 0, 0, 0 )
      val d = Vector3( 0, 0, -1 )

      val r = Ray( o, d )

      assert( r.o == o )
      assert( r.d == d )
    }

    it( "should throw an exception if null is passed as origin" ) {
      val d = Vector3( 0, 0, -1 )
      intercept[RuntimeException] {
        Ray( null, d )
      }
    }

    it( "should throw an exception if null is passed as direction" ) {
      val o  = Point3( 0, 0, 0 )
      intercept[RuntimeException] {
        Ray( o, null )
      }
    }

    it( "should have an apply method that takes a double value as parameter and calculates a point" )(pending)

    it( "should have an apply method that takes a point value as parameter and calculates the t of the point" )(pending)

    it( "should have a \"shoot-the-ray\" operator that take a geometry as parameter and calls the operator on that geometry" )(pending)

    it( "should have a \"shoot-the-ray\" operator that take a world as parameter and calls the operator on that world" )(pending)

    it( "should not be altered by the apply method to calculate a point" )(pending)

    it( "should not be altered by the apply method to calculate a t" )(pending)

    it( "should not be altered by \"shoot-the-ray\" operator for a geometry" )(pending)

    it( "should not be altered by \"shoot-the-ray\" operator for a world" )(pending)


  }

}
