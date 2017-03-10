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
package org.lappsgrid.jupyter;

import com.twosigma.beaker.evaluator.Evaluator;
import com.twosigma.beaker.jupyter.Comm;
import org.lappsgrid.jupyter.msg.Message;
import org.zeromq.ZMQ;

import java.security.NoSuchAlgorithmException;
import java.util.Observer;
import java.util.Set;

public interface KernelFunctionality {

  void publish(Message message) throws NoSuchAlgorithmException;

  void addComm(String commId, Comm comm);

  void removeComm(String commId);

  void send(Message message) throws NoSuchAlgorithmException;

  void send(final ZMQ.Socket socket, Message message) throws NoSuchAlgorithmException;

  String getId();

  Observer getExecutionResultSender();

  Comm getComm(String string);

  Evaluator getEvaluator(Kernel kernel);
  
  boolean isCommPresent(String string);

  Set<String> getCommHashSet();

  void setShellOptions(String usString, String usString1, String o);

  void cancelExecution();
}