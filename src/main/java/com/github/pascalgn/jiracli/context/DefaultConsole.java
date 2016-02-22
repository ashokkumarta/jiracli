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
package com.github.pascalgn.jiracli.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DefaultConsole implements Console {
    private final BufferedReader reader;

    public DefaultConsole() {
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void print(String str) {
        System.out.print(str);
        System.out.flush();
    }

    @Override
    public void println(String str) {
        System.out.println(str);
    }

    @Override
    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Error reading line!", e);
        }
    }

    @Override
    public char[] readPassword() {
        java.io.Console console = System.console();
        if (console == null) {
            String str = readLine();
            return (str == null ? null : str.toCharArray());
        } else {
            return console.readPassword();
        }
    }
}