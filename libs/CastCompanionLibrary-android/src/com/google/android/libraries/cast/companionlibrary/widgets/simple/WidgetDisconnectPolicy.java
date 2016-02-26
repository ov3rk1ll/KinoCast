/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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

package com.google.android.libraries.cast.companionlibrary.widgets.simple;

/**
 * An enum that defines various behaviors when we are disconnected from the cast device.
 */
public enum WidgetDisconnectPolicy {
    INVISIBLE(1), GONE(2), DISABLED(3), VISIBLE(4);

    private int mValue;

    WidgetDisconnectPolicy(int value) {
        mValue = value;
    }

    public int getValue() {
        return mValue;
    }

    public static WidgetDisconnectPolicy fromValue(int value) {
        switch (value) {
            case 1:
                return INVISIBLE;
            case 2:
                return GONE;
            case 3:
                return DISABLED;
            default:
                return VISIBLE;
        }
    }

}
