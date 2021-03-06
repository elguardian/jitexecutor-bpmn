/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.jitexecutor.process;

import java.util.ArrayList;
import java.util.List;

import org.kie.kogito.Model;
import org.kie.kogito.process.Process;

public class ProcessBuild {

    private String processId;

    private Process<? extends Model> process;

    private List<String> errors;

    public ProcessBuild(String processId) {
        this.errors = new ArrayList<>();
        this.processId = processId;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> errors() {
        return errors;
    }

    public String id() {
        return processId;
    }

    public void setProcess(Process<? extends Model> process) {
        this.process = process;
    }

    public Process<? extends Model> process() {
        return process;
    }

    @Override
    public String toString() {
        return "[" + processId + ", valid: " + hasErrors() + "]";
    }

}
