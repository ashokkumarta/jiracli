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

import java.util.Collection;

import com.github.pascalgn.jiracli.context.Context;
import com.github.pascalgn.jiracli.model.Data;
import com.github.pascalgn.jiracli.model.Issue;
import com.github.pascalgn.jiracli.model.IssueList;
import com.github.pascalgn.jiracli.util.Function;

@CommandDescription(names = "links", description = "Show all issues linked with the given issues")
class Links implements Command {
    @Override
    public Data execute(final Context context, Data input) {
        IssueList issueList = input.toIssueListOrFail();
        return new IssueList(issueList.loadingSupplier(new Function<Issue, Collection<Issue>>() {
            @Override
            public Collection<Issue> apply(Issue issue) {
                return context.getWebService().getLinks(issue);
            }
        }));
    }
}