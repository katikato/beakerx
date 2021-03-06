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
package com.twosigma.beaker.jupyter.msg;

import static com.twosigma.beaker.jupyter.Utils.timestamp;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.EXECUTE_REPLY;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.EXECUTE_RESULT;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.STATUS;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.STREAM;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.CLEAR_OUTPUT;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.DISPLAY_DATA;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;

import com.twosigma.beaker.jvm.object.ConsoleOutput;
import com.twosigma.beaker.jvm.object.OutputCell;
import com.twosigma.beaker.jvm.object.SimpleEvaluationObject;
import com.twosigma.beaker.mimetype.MIMEContainer;
import com.twosigma.jupyter.KernelFunctionality;
import com.twosigma.jupyter.message.Header;
import com.twosigma.jupyter.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twosigma.beaker.SerializeToString;
import com.twosigma.beaker.jvm.object.SimpleEvaluationObject.EvaluationStatus;
import com.twosigma.beaker.jupyter.SocketEnum;

/**
 * Converts SimpleEvaluationObject to Message
 *
 * @author konst
 */
public class MessageCreator {

  public static final String EXECUTION_STATE = "execution_state";
  public static final String BUSY = "busy";
  public static final String IDLE = "idle";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String NULL_RESULT = "null";

  public static Logger logger = LoggerFactory.getLogger(MessageCreator.class);
  protected KernelFunctionality kernel;

  public MessageCreator(KernelFunctionality kernel) {
    this.kernel = kernel;
  }

  private Message initMessage(JupyterMessages type, Message message) {
    Message reply = new Message();
    reply.setParentHeader(message.getHeader());
    reply.setIdentities(message.getIdentities());
    reply.setHeader(new Header(type, message.getHeader().getSession()));
    return reply;
  }

  public Message buildMessage(Message message, String mime, String code, int executionCount) {
    Message reply = initMessage(EXECUTE_RESULT, message);
    reply.setContent(new HashMap<String, Serializable>());
    reply.getContent().put("execution_count", executionCount);
    HashMap<String, String> map3 = new HashMap<>();
    map3.put(mime, code);
    reply.getContent().put("data", map3);
    reply.getContent().put("metadata", new HashMap<>());
    return reply;
  }

  public Message buildClearOutput(Message message, boolean wait) {
    Message reply = initMessage(CLEAR_OUTPUT, message);
    reply.setContent(new HashMap<String, Serializable>());
    reply.getContent().put("wait", wait);
    reply.getContent().put("metadata", new HashMap<>());
    return reply;
  }

  public Message buildDisplayData(Message message, MIMEContainer value) {
    Message reply = initMessage(DISPLAY_DATA, message);
    reply.setContent(new HashMap<String, Serializable>());
    reply.getContent().put("metadata", new HashMap<>());
    HashMap<String, Serializable> map3 = new HashMap<>();
    map3.put(value.getMime(), value.getCode());
    reply.getContent().put("data", map3);
    return reply;
  }

  private Message buildReply(Message message, SimpleEvaluationObject seo) {
    // Send the REPLY to the original message. This is NOT the result of
    // executing the cell. This is the equivalent of 'exit 0' or 'exit 1'
    // at the end of a shell script.
    Message reply = buildReplyWithoutStatus(message, seo.getExecutionCount());
    if (EvaluationStatus.FINISHED == seo.getStatus()) {
      reply.getMetadata().put("status", "ok");
      reply.getContent().put("status", "ok");
      reply.getContent().put("user_expressions", new HashMap<>());
    } else if (EvaluationStatus.ERROR == seo.getStatus()) {
      reply.getMetadata().put("status", "error");
      reply.getContent().put("status", "error");
    }
    return reply;
  }

  private Message buildReplyWithoutStatus(Message message, int executionCount) {
    Message reply = initMessage(EXECUTE_REPLY, message);
    Hashtable<String, Serializable> map6 = new Hashtable<String, Serializable>(3);
    map6.put("dependencies_met", true);
    map6.put("engine", kernel.getSessionId());
    map6.put("started", timestamp());
    reply.setMetadata(map6);
    Hashtable<String, Serializable> map7 = new Hashtable<String, Serializable>(1);
    map7.put("execution_count", executionCount);
    reply.setContent(map7);
    return reply;
  }

  public Message buildOutputMessage(Message message, String text, boolean hasError) {
    Message reply = initMessage(STREAM, message);
    reply.setContent(new HashMap<String, Serializable>());
    reply.getContent().put("name", hasError ? "stderr" : "stdout");
    reply.getContent().put("text", text);
    logger.info("Console output:", "Error: " + hasError, text);
    return reply;
  }

  public synchronized void createMagicMessage(Message reply, int executionCount, Message message) {
    kernel.publish(reply);
    kernel.publish(buildReplyWithoutStatus(message, executionCount));
  }

  public synchronized List<MessageHolder> createMessage(SimpleEvaluationObject seo) {
    logger.info("Creating message response message from: " + seo);
    Message message = seo.getJupyterMessage();
    List<MessageHolder> ret = new ArrayList<>();
    if (isConsoleOutputMessage(seo)) {
      ret.addAll(createConsoleResult(seo, message));
    } else if (isSupportedStatus(seo.getStatus())) {
      ret.addAll(createResultForSupportedStatus(seo, message));
    } else {
      logger.error("Unhandled status of SimpleEvaluationObject : " + seo.getStatus());
    }
    return ret;
  }

  private List<MessageHolder> createResultForSupportedStatus(SimpleEvaluationObject seo, Message message) {
    List<MessageHolder> ret = new ArrayList<>();
    if (EvaluationStatus.FINISHED == seo.getStatus() && showResult(seo)) {
      ret.add(createFinishResult(seo, message));
    } else if (EvaluationStatus.ERROR == seo.getStatus()) {
      ret.add(createErrorResult(seo, message));
    }
    ret.add(new MessageHolder(SocketEnum.IOPUB_SOCKET, createIdleMessage(message)));
    ret.add(new MessageHolder(SocketEnum.SHELL_SOCKET, buildReply(message, seo)));
    return ret;
  }

  private boolean showResult(SimpleEvaluationObject seo) {
    return !OutputCell.HIDDEN.equals(seo.getPayload());
  }

  private boolean isSupportedStatus(EvaluationStatus status) {
    return EvaluationStatus.FINISHED == status || EvaluationStatus.ERROR == status;
  }

  private boolean isConsoleOutputMessage(SimpleEvaluationObject seo) {
    return seo.getConsoleOutput() != null && !seo.getConsoleOutput().isEmpty();
  }

  private List<MessageHolder> createConsoleResult(SimpleEvaluationObject seo, Message message) {
    List<MessageHolder> result = new ArrayList<>();
    while (!seo.getConsoleOutput().isEmpty()) {
      ConsoleOutput co = seo.getConsoleOutput().poll(); //FIFO : peek to see, poll -- removes the data
      result.add(new MessageHolder(SocketEnum.IOPUB_SOCKET, buildOutputMessage(message, co.getText(), co.isError())));
    }
    return result;
  }

  private MessageHolder createErrorResult(SimpleEvaluationObject seo, Message message) {
    logger.info("Execution result ERROR: " + seo.getPayload().toString().split("\n")[0]);
    Message reply = initMessage(STREAM, message);
    Hashtable<String, Serializable> map4 = new Hashtable<String, Serializable>(2);
    map4.put("name", "stderr");
    map4.put("text", (String) seo.getPayload());
    reply.setContent(map4);
    return new MessageHolder(SocketEnum.IOPUB_SOCKET, reply);
  }

  private MessageHolder createFinishResult(SimpleEvaluationObject seo, Message message) {
    MIMEContainer resultString = SerializeToString.doit(seo.getPayload());
    return new MessageHolder(
            SocketEnum.IOPUB_SOCKET,
            buildMessage(
                    message,
                    resultString.getMime(),
                    resultString.getCode(),
                    seo.getExecutionCount()));
  }

  public Message createBusyMessage(Message parentMessage) {
    return getExecutionStateMessage(parentMessage, BUSY);
  }

  public Message createIdleMessage(Message parentMessage) {
    return getExecutionStateMessage(parentMessage, IDLE);
  }

  private Message getExecutionStateMessage(Message parentMessage, String state) {
    Map<String, Serializable> map1 = new HashMap<String, Serializable>(1);
    map1.put(EXECUTION_STATE, state);
    Message reply = initMessage(STATUS, parentMessage);
    reply.setContent(map1);
    return reply;
  }

}