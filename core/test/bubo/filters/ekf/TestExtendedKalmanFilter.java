/*
 * Copyright (c) 2013-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Project BUBO.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bubo.filters.ekf;

import bubo.filters.GenericKalmanFilterTests;
import bubo.filters.MultivariateGaussianDM;
import bubo.filters.abst.KalmanFilterInterface;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestExtendedKalmanFilter extends GenericKalmanFilterTests {
	private double T = 0.1;

	/**
	 * Run all the generic automated tests on the EKF
	 */
	@Test
	public void performGenericTests() {
		performAllGenericTests();
	}

	/**
	 * See if the propagation function produces the expected results.
	 * <p/>
	 * The test results where hand computed
	 */
	@Test
	public void checkPropagation() {
		MultivariateGaussianDM x_init = createInitialState();

		double x = x_init.getMean().get(0, 0);
		double v = x_init.getMean().get(1, 0);
		double e = x_init.getMean().get(2, 0);

		DenseMatrix64F t = new DenseMatrix64F(3, 1);
		t.set(0, 0, x + v * T);
		t.set(1, 0, v);
		t.set(2, 0, Math.cos(Math.acos(e) + T));

		checkPropagation(t, 1e-5);
	}

	@Override
	protected KalmanFilterInterface createFilter() {

		EkfPredictorDiscrete propagator = new OscillatingConstMotion(T);
		EkfProjector projector = new Projector();

		return new ExtendedKalmanFilter(propagator, projector);
	}

	@Override
	protected MultivariateGaussianDM createInitialState() {
		return createState(0.5, 2, 3, 0);
	}

	@Override
	protected DenseMatrix64F createTargetState() {
		ExtendedKalmanFilter filter = (ExtendedKalmanFilter) createFilter();
		EkfPredictorDiscrete prop = filter.getPredictor();

		MultivariateGaussianDM ret = createInitialState();

		prop.compute(ret.getMean());

		return new DenseMatrix64F(prop.getPredictedState());
	}

	@Override
	protected MultivariateGaussianDM createPerfectMeas(KalmanFilterInterface filter,
													   DenseMatrix64F state) {
		EkfProjector proj = ((ExtendedKalmanFilter) filter).getProjector();

		proj.compute(state);
		DenseMatrix64F z = proj.getProjected();
		DenseMatrix64F R = CommonOps.identity(2);

		return new MultivariateGaussianDM(z, R);
	}


	/**
	 * Creates a projector using a sensor described by:
	 * <p/>
	 * h(x) = [x1 + x3 ; x1x2 ]
	 */
	private static class Projector implements EkfProjector {
		DenseMatrix64F z = new DenseMatrix64F(2, 1);
		DenseMatrix64F H = new DenseMatrix64F(2, 3);


		@Override
		public int getSystemSize() {
			return 3;
		}

		@Override
		public int getMeasurementSize() {
			return 2;
		}

		public void compute(DenseMatrix64F state) {
			double x1 = state.get(0, 0);
			double x2 = state.get(1, 0);
			double x3 = state.get(2, 0);

			z.set(0, 0, x1 + x3);
			z.set(1, 0, x2 * x2);

			H.set(0, 0, 1.0);
			H.set(0, 2, 1.0);
			H.set(1, 1, 2 * x2);
		}

		@Override
		public DenseMatrix64F getJacobianH() {
			return H;
		}

		@Override
		public DenseMatrix64F getProjected() {
			return z;
		}
	}
}
