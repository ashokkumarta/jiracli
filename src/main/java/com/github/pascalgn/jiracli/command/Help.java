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
package com.github.pascalgn.jiracli.command;

import com.github.pascalgn.jiracli.command.CommandFactory.CommandDescriptor;
import com.github.pascalgn.jiracli.context.Console;
import com.github.pascalgn.jiracli.context.Context;
import com.github.pascalgn.jiracli.model.Data;
import com.github.pascalgn.jiracli.model.None;

@CommandDescription(names = { "help", "h", "?" }, description = "Show a list of available commands")
public class Help implements Command {
    @Override
    public Data<?> execute(Context context, Data<?> input) {
        Console console = context.getConsole();
        CommandFactory commandFactory = CommandFactory.getInstance();
        StringBuilder str = new StringBuilder();
        for (CommandDescriptor commandDescriptor : commandFactory.getCommandDescriptors()) {
            str.append(CommandUtils.join(commandDescriptor.getNames(), ", "));
            str.append(System.lineSeparator());
            str.append("    ");
            str.append(commandDescriptor.getDescription());
            str.append(System.lineSeparator());
        }
        console.print(str.toString());
        return None.getInstance();
    }
}