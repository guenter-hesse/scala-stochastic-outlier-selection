package com.github.gnni.outlierdetection

import breeze.linalg.{DenseVector, sum}
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest._

// Unit-tests created based on the Python script of https://github.com/jeroenjanssens/sos
class StocasticOutlierDetectionTest extends FlatSpec with Matchers with BeforeAndAfter {
  val perplexity = 3.0
  val epsilon = 1e-9f
  implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(epsilon)

  "Computing the distance matrix " should "give symmetrical distances" in {

    val data = Array(
        Array(1.0, 3.0),
        Array(5.0, 1.0)
      )

    val dMatrix = StochasticOutlierDetection.computeDistanceMatrix(data).map(_._2).sortBy(dist => sum(dist))

    dMatrix(0) should be(dMatrix(1))
  }

  "Computing the distance matrix " should "give the correct distances" in {

    val data = 
      Array(
        Array(1.0, 1.0),
        Array(2.0, 2.0),
        Array(5.0, 1.0)
      )

    val dMatrix = StochasticOutlierDetection.computeDistanceMatrix(data).map(_._2).sortBy(dist => sum(dist))

    dMatrix(0) should be(Array(Math.sqrt(2.0), Math.sqrt(10.0)))
    dMatrix(1) should be(Array(Math.sqrt(2.0), Math.sqrt(16.0)))
    dMatrix(2) should be(Array(Math.sqrt(16.0), Math.sqrt(10.0)))
  }

  "Computing the affinity matrix " should "give the correct affinity" in {

    // The datapoints
    val data = 
      Array(
        Array(1.0, 1.0),
        Array(2.0, 1.0),
        Array(1.0, 2.0),
        Array(2.0, 2.0),
        Array(5.0, 8.0) // The outlier!
      )

    val dMatrix = StochasticOutlierDetection.computeDistanceMatrix(data)
    val aMatrix = StochasticOutlierDetection.computeAffinityMatrix( dMatrix,
                                                                    perplexity,
                                                                    StochasticOutlierDetection.DefaultIterations).map(_._2).sortBy(dist => sum(dist))

    assert(aMatrix.length == 5)
    assert(aMatrix(0)(0) === 1.65024581e-06)
    assert(aMatrix(0)(1) === 3.44967758e-06)
    assert(aMatrix(0)(2) === 6.73004970e-06)
    assert(aMatrix(0)(3) === 1.54422167e-05)

    assert(aMatrix(1)(0) === 2.83704489e-01)
    assert(aMatrix(1)(1) === 4.10315559e-01)
    assert(aMatrix(1)(2) === 4.10315559e-01)
    assert(aMatrix(1)(3) === 2.53931482e-03)

    assert(aMatrix(2)(0) === 4.31925256e-01)
    assert(aMatrix(2)(1) === 3.05063253e-01)
    assert(aMatrix(2)(2) === 4.31925256e-01)
    assert(aMatrix(2)(3) === 2.34905952e-03)

    assert(aMatrix(3)(0) === 4.48046267e-01)
    assert(aMatrix(3)(1) === 3.21289154e-01)
    assert(aMatrix(3)(2) === 4.48046267e-01)
    assert(aMatrix(3)(3) === 2.21082335e-03)

    assert(aMatrix(4)(0) === 4.64662765e-01)
    assert(aMatrix(4)(1) === 4.64662765e-01)
    assert(aMatrix(4)(2) === 3.38268739e-01)
    assert(aMatrix(4)(3) === 2.07195221e-03)
  }

  "Verify the binding probabilities " should "give the correct probabilities" in {

    // The distance matrix
    val dMatrix = 
      Array(
        (0L, new DenseVector(Array(6.61626106e-112, 1.27343495e-088))),
        (1L, new DenseVector(Array(2.21858114e-020, 1.12846575e-044))),
        (2L, new DenseVector(Array(1.48949023e-010, 1.60381089e-028)))
      )

    val bMatrix = StochasticOutlierDetection.computeBindingProbabilities(dMatrix).map(_._2).sortBy(dist => sum(dist))

    assert(bMatrix(0)(0) === 5.19560192e-24)
    assert(bMatrix(0)(1) === 1.00000000e+00)

    assert(bMatrix(1)(0) === 1.00000000e+00)
    assert(bMatrix(1)(1) === 5.08642993e-25)

    assert(bMatrix(2)(0) === 1.00000000e+00)
    assert(bMatrix(2)(1) === 1.07675154e-18)
  }

  "Verifying the product " should "should provide valid products" in {

    val data = 
      Array(
        (0L, Array(0.5, 0.3)),
        (1L, Array(0.25, 0.1)),
        (2L, Array(0.8, 0.8))
      )

    val oMatrix = StochasticOutlierDetection.computeOutlierProbability(data).map(_._2).sortBy(dist => dist)

    val out0 = (1.0 - 0.5) * (1.0 - 0.0) * (1.0 - 0.8)
    val out1 = (1.0 - 0.0) * (1.0 - 0.25) * (1.0 - 0.8)
    val out2 = (1.0 - 0.3) * (1.0 - 0.1) * (1.0 - 0)

    assert(oMatrix.length == 3)

    assert(oMatrix(0) === out0)
    assert(oMatrix(1) === out1)
    assert(oMatrix(2) === out2)
  }

  "Verifying the output of the SOS algorithm " should "assign the one true outlier" in {

    // The distance matrix
    val data = 
      Array(
        Array(1.0, 1.0),
        Array(2.0, 1.0),
        Array(1.0, 2.0),
        Array(2.0, 2.0),
        Array(5.0, 8.0) // The outlier!
      )

    // Process the steps of the algorithm
    val dMatrix = StochasticOutlierDetection.computeDistanceMatrix(data)

    val aMatrix = StochasticOutlierDetection.computeAffinityMatrix( dMatrix,
                                                                    perplexity,
                                                                    StochasticOutlierDetection.DefaultIterations)

    val bMatrix = StochasticOutlierDetection.computeBindingProbabilities(aMatrix)

    val oMatrix = StochasticOutlierDetection.computeOutlierProbability(bMatrix)

    // Do a distributed sort, and then return to driver
    val output = oMatrix.map(_._2).sortBy(rank => rank)

    assert(output.length == 5)
    assert(output(0) === 0.12707053787018440794)
    assert(output(1) === 0.22136130977995771563)
    assert(output(2) === 0.25775014551682556840)
    assert(output(3) === 0.27900944792028958830)
    assert(output(4) === 0.99227799024537555184) // The outlier!
  }

  "Verifying the output of the SOS algorithm by calling performOutlierDetection() " should "assign the one true outlier" in {

    // The distance matrix
    val data =
      Array(
        Array(1.0, 1.0),
        Array(2.0, 1.0),
        Array(1.0, 2.0),
        Array(2.0, 2.0),
        Array(5.0, 8.0) // The outlier!
      )

    val oMatrix = StochasticOutlierDetection.performOutlierDetection(data, perplexity)

    // Do a distributed sort, and then return to driver
    val output = oMatrix.map(_._2).sortBy(rank => rank)

    assert(output.length == 5)
    assert(output(0) === 0.12707053787018440794)
    assert(output(1) === 0.22136130977995771563)
    assert(output(2) === 0.25775014551682556840)
    assert(output(3) === 0.27900944792028958830)
    assert(output(4) === 0.99227799024537555184) // The outlier!
  }

}