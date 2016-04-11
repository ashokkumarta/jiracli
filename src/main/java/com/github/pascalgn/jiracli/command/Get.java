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

import com.github.pascalgn.jiracli.command.Argument.Parameters;
import com.github.pascalgn.jiracli.context.Context;
import com.github.pascalgn.jiracli.model.Converter;
import com.github.pascalgn.jiracli.model.Data;
import com.github.pascalgn.jiracli.model.Field;
import com.github.pascalgn.jiracli.model.Issue;
import com.github.pascalgn.jiracli.model.IssueList;
import com.github.pascalgn.jiracli.model.Schema;
import com.github.pascalgn.jiracli.model.Text;
import com.github.pascalgn.jiracli.model.TextList;
import com.github.pascalgn.jiracli.util.Function;

@CommandDescription(names = "get", description = "Return the field value for the given field")
class Get implements Command {
    @Argument(parameters = Parameters.ONE, variable = "<field>", description = "the field ID")
    private String field;

    @Override
    public TextList execute(final Context context, Data input) {
        Schema schema = context.getWebService().getSchema();
        final Converter converter = schema.getConverter(field);
        IssueList issueList = input.toIssueListOrFail();
        return new TextList(issueList.convertingSupplier(new Function<Issue, Text>() {
            @Override
            public Text apply(Issue issue) {
                Field f = issue.getFieldMap().getFieldById(field);
                if (f == null) {
                    throw new IllegalArgumentException("Unknown field: " + field);
                }
                Object val = f.getValue().get();
                String str = converter.toString(val);
                return new Text(str);
            }
        }));
    }
}
