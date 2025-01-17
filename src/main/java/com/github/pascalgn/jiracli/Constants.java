/**
 * Copyright 2016 Pascal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pascalgn.jiracli;

import java.util.prefs.Preferences;

/**
 * Shared constants
 */
public class Constants {
    public static String getName() {
        return "Jiracli";
    }

    public static String getVersion() {
        return "1.3.0-SNAPSHOT";
    }

    public static String getTitle() {
        return getName() + " " + getVersion();
    }

    public static Preferences getPreferences() {
        return Preferences.userNodeForPackage(Constants.class);
    }
}
