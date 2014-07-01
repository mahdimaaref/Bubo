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

package bubo.models.kinematics;

import bubo.filters.ekf.EkfPredictorTime;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * <p>
 * Kinematics for a 2D robot which is controlled by translational and angular velocity controls.
 * </p>
 * <p>
 * Motion Model:
 * <pre>
 * [ x' ]   [ x ]   [ -v/w*sin(&theta;) + v/w*sin(&theta; + w*&Delta;t ]
 * [ y' ] = [ y ] + [ v/w*cos(&theta;) - v/w*cos(&theta; + w*&Delta;t ]
 * [ &theta;' ]   [ &theta; ]   [ w*&Delta;t + &gamma;*&Delta;t ]
 * </pre>
 * where v and w are translation and rotational velocity
 * </p>
 * <p/>
 * <p>
 * [1] Page 129, S. Thrun, W. Burgard, D. Fox, "Probabilistic Robotics" MIT Press 2006
 * </p>
 *
 * @author Peter Abeles
 */
public class PredictorRobotVelocity2D implements EkfPredictorTime {

	// estimated state
	DenseMatrix64F x_est = new DenseMatrix64F(3, 1);
	// state transition matrix
	DenseMatrix64F G = CommonOps.identity(3);
	// plant noise covariance matrix
	DenseMatrix64F Q = new DenseMatrix64F(3, 3);

	// Control noise model
	DenseMatrix64F M = new DenseMatrix64F(2, 2);
	// Control Jacobian
	DenseMatrix64F V = new DenseMatrix64F(3, 2);
	// holds intermediate results
	DenseMatrix64F tempVM = new DenseMatrix64F(3, 2);

	// control input: Translational Velocity
	double vel;
	// control input: Angular Velocity
	double velAngle;
	// robot parameters
	double a1, a2, a3, a4;

	/**
	 * Configures robot motion model
	 *
	 * @param a1 Robot specific parameter 1
	 * @param a2 Robot specific parameter 2
	 * @param a3 Robot specific parameter 3
	 * @param a4 Robot specific parameter 4
	 */
	public PredictorRobotVelocity2D(double a1, double a2, double a3, double a4) {
		this.a1 = a1;
		this.a2 = a2;
		this.a3 = a3;
		this.a4 = a4;
	}

	/**
	 * Specify velocity controls.
	 *
	 * @param translationalVel translational velocity
	 * @param angularVel       angular velocity
	 */
	public void setControl(double translationalVel, double angularVel) {
		this.vel = translationalVel;
		this.velAngle = angularVel;
	}

	@Override
	public int getSystemSize() {
		return 3;
	}

	@Override
	public void compute(DenseMatrix64F state, double T) {
		double x = state.get(0);
		double y = state.get(1);
		double theta = state.get(2);

		double s = Math.sin(theta);
		double c = Math.cos(theta);

		if (velAngle == 0.0) {
			// handle pathological case where there is no curvature
			x_est.data[0] = x + vel * c * T;
			x_est.data[1] = y + vel * s * T;
			x_est.data[2] = theta;

			G.unsafe_set(0, 2, -vel * s * T);
			G.unsafe_set(1, 2, vel * c * T);

			V.unsafe_set(0, 0, c * T);
			V.unsafe_set(0, 1, 0);
			V.unsafe_set(1, 0, s * T);
			V.unsafe_set(1, 1, 0);
			V.unsafe_set(2, 1, T);
		} else {
			double vDw = vel / velAngle;
			double dTheta = velAngle * T;

			double sp = Math.sin(theta + dTheta);
			double cp = Math.cos(theta + dTheta);

			// compute predicted state
			x_est.data[0] = x + vDw * (-s + sp);
			x_est.data[1] = y + vDw * (c - cp);
			x_est.data[2] = theta + dTheta;

			// calculate state jacobian
			G.unsafe_set(0, 2, vDw * (-c + cp));
			G.unsafe_set(1, 2, vDw * (-s + sp));

			V.unsafe_set(0, 0, (-s + sp) / velAngle);
			V.unsafe_set(0, 1, (vel / velAngle) * ((s - sp) / velAngle + cp * T));
			V.unsafe_set(1, 0, (c - cp) / velAngle);
			V.unsafe_set(1, 1, (vel / velAngle) * ((c - cp) / velAngle + sp * T));
			V.unsafe_set(2, 1, T);
		}

		M.unsafe_set(0, 0, a1 * vel * vel + a2 * velAngle * velAngle);
		M.unsafe_set(1, 1, a3 * vel * vel + a4 * velAngle * velAngle);

		CommonOps.mult(V, M, tempVM);
		CommonOps.multTransB(tempVM, V, Q);
	}

	@Override
	public DenseMatrix64F getJacobianF() {
		return G;
	}

	@Override
	public DenseMatrix64F getPlantNoise() {
		return Q;
	}

	@Override
	public DenseMatrix64F getPredictedState() {
		return x_est;
	}

	public DenseMatrix64F getM() {
		return M;
	}

	public DenseMatrix64F getV() {
		return V;
	}
}
