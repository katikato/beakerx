/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beaker.easyform.formitem.widgets;

import com.twosigma.beaker.easyform.formitem.ListComponent;
import com.twosigma.beaker.jupyter.Comm;
import com.twosigma.beaker.widgets.CommFunctionality;
import com.twosigma.beaker.widgets.selections.SelectMultiple;

import java.util.Collection;

public class ListComponentWidget extends ListComponent implements CommFunctionality {

  private SelectMultiple widget;

  public ListComponentWidget() {
    this.widget = new SelectMultiple();
  }

  @Override
  public Comm getComm() {
    return widget.getComm();
  }

  @Override
  public void setLabel(String label) {
    super.setLabel(label);
    this.widget.setDescription(label);
  }

  @Override
  public void setValues(Collection<String> values) {
    super.setValues(values);
    this.widget.setOptions(values.stream().toArray(String[]::new));
  }

  public void setValue(String[] value) {
    this.widget.setValue(value);
  }
}
