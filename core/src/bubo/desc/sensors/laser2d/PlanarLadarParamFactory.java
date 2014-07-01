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

package bubo.desc.sensors.laser2d;

/**
 * Creates parameters for common LADARs.
 *
 * @author Peter Abeles
 */
public class PlanarLadarParamFactory {

	public static PlanarLadarParam createSickLms200() {
		PlanarLadarParam param = new PlanarLadarParam();

		param.setStartAngle(-Math.PI / 2.0);
		param.setAngleIncrement(Math.PI / 360.0);
		param.setNumberOfScans(361);
		param.setMaxRange(7.5);

		return param;
	}

	public static PlanarLadarParam createHokuyo() {
		PlanarLadarParam param = new PlanarLadarParam();

		int frontStep = 384;
		int initialStep = 44;
		int finalStep = 725;

		param.setAngleIncrement(2.0 * Math.PI / 1024.0);
		param.setStartAngle(-param.getAngleIncrement() * (frontStep - initialStep));
		param.setNumberOfScans(finalStep - initialStep + 1);
		param.setMaxRange(5.5);

		return param;
	}
}
