/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package httl.spi.parsers.template;

import httl.Context;
import httl.Engine;
import httl.Template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

/**
 * Adaptive Template. (SPI, Prototype, ThreadSafe)
 * 
 * @see httl.Engine#getTemplate(String)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public class AdaptiveTemplate implements Template, Serializable {

	private static final long serialVersionUID = 3094907176375413567L;

	private final Template writerTemplate;

	private final Template streamTemplate;

	public AdaptiveTemplate(Template writerTemplate, Template streamTemplate) {
		if (writerTemplate == null)
			throw new IllegalArgumentException("writer template == null");
		if (streamTemplate == null)
			throw new IllegalArgumentException("stream template == null");
		this.writerTemplate = writerTemplate;
		this.streamTemplate = streamTemplate;
	}

	public String getName() {
		return writerTemplate.getName();
	}

	public String getEncoding() {
		return writerTemplate.getEncoding();
	}

	public long getLastModified() {
		return writerTemplate.getLastModified();
	}

	public long getLength() {
		return writerTemplate.getLength();
	}

	public String getSource() {
		return writerTemplate.getSource();
	}

	public Reader getReader() throws IOException {
		return writerTemplate.getReader();
	}

	public InputStream getInputStream() throws IOException {
		return streamTemplate.getInputStream();
	}

	public Engine getEngine() {
		return writerTemplate.getEngine();
	}

    public Class<?> getReturnType() {
    	if (Context.getContext().getOutput() instanceof OutputStream) {
			return streamTemplate.getReturnType();
		} else {
			return writerTemplate.getReturnType();
		}
    }

	public Object evaluate(Map<String, Object> parameters) {
		if (Context.getContext().getOutput() instanceof OutputStream) {
			return streamTemplate.evaluate(parameters);
		} else {
			return writerTemplate.evaluate(parameters);
		}
	}

	public void render(Map<String, Object> parameters, OutputStream output)
			throws IOException {
		streamTemplate.render(parameters, output);
	}

	public void render(Map<String, Object> parameters, Writer writer)
			throws IOException {
		writerTemplate.render(parameters, writer);
	}

	public Map<String, Class<?>> getParameterTypes() {
		return writerTemplate.getParameterTypes();
	}

	public Map<String, Class<?>> getContextTypes() {
		return writerTemplate.getContextTypes();
	}

	public Map<String, Template> getMacros() {
		return writerTemplate.getMacros();
	}

	public String getCode() {
		return writerTemplate.getCode();
	}

	public int getOffset() {
		return writerTemplate.getOffset();
	}

	public boolean isMacro() {
		return writerTemplate.isMacro();
	}

}
