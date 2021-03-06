/*
 * Copyright (C) 2016 Hugo Matalonga & João Paulo Fernandes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmatalonga.greenhub.managers.sampling;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

/**
 * Created by hugo on 13-04-2016.
 */
public class SignalListener extends PhoneStateListener {

    private int mGsmSignal = 0;
    private int mEvdoDbm = 0;
    private int mCdmaDbm = 0;

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        // TODO Auto-generated method stub
        super.onSignalStrengthsChanged(signalStrength);
        mGsmSignal = signalStrength.getGsmSignalStrength();
        mCdmaDbm = signalStrength.getCdmaDbm();
        mEvdoDbm = signalStrength.getEvdoDbm();
    }

    public int getmGsmSignal() {
        return mGsmSignal;
    }

    public int getmEvdoDbm() {
        return mEvdoDbm;
    }

    public int getmCdmaDbm() {
        return mCdmaDbm;
    }
}