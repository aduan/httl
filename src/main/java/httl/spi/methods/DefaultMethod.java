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
package httl.spi.methods;

import httl.Context;
import httl.Engine;
import httl.Expression;
import httl.Resource;
import httl.Template;
import httl.spi.Formatter;
import httl.spi.Logger;
import httl.spi.Resolver;
import httl.spi.methods.cycles.ArrayCycle;
import httl.spi.methods.cycles.BooleanArrayCycle;
import httl.spi.methods.cycles.ByteArrayCycle;
import httl.spi.methods.cycles.CharArrayCycle;
import httl.spi.methods.cycles.DoubleArrayCycle;
import httl.spi.methods.cycles.FloatArrayCycle;
import httl.spi.methods.cycles.IntArrayCycle;
import httl.spi.methods.cycles.ListCycle;
import httl.spi.methods.cycles.LongArrayCycle;
import httl.spi.methods.cycles.ShortArrayCycle;
import httl.util.ClassUtils;
import httl.util.DateUtils;
import httl.util.EncodingProperties;
import httl.util.IOUtils;
import httl.util.LocaleUtils;
import httl.util.MD5;
import httl.util.NumberUtils;
import httl.util.StringUtils;
import httl.util.UrlUtils;
import httl.util.WrappedMap;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fastjson.JSON;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * DefaultMethod. (SPI, Singleton, ThreadSafe)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public class DefaultMethod {

    private static final Random RANDOM = new Random();

    private Engine engine;

	private Resolver resolver;

	private Logger logger;

	private Formatter<Object> formatter;

	private TimeZone timeZone;

    private String dateFormat;

    private String numberFormat;

    private String outputEncoding;

    private Charset outputCharset;

	private String[] importPackages;

	private String i18nBasename;
	
	private String i18nFormat;

	private String i18nEncoding;
	
	private boolean reloadable;

	/**
	 * httl.properties: reloadable=true
	 */
	public void setReloadable(boolean reloadable) {
		this.reloadable = reloadable;
	}

	/**
	 * httl.properties: i18n.encoding=UTF-8
	 */
	public void setI18nEncoding(String i18nEncoding) {
		this.i18nEncoding = i18nEncoding;
	}

	/**
	 * httl.properties: i18n.basename=messages
	 */
	public void setI18nBasename(String i18nBasename) {
		this.i18nBasename = i18nBasename;
	}

	/**
	 * httl.properties: i18n.format=string
	 */
	public void setI18nFormat(String i18nFormat) {
		if (! "string".equals(i18nFormat)
				&& ! "message".equals(i18nFormat)) {
			throw new IllegalArgumentException("Unsupported i18n.format=" + i18nFormat + ", only supported \"string\" or \"message\" format.");
		}
		this.i18nFormat = i18nFormat;
	}

	/**
	 * httl.properties: formatter=httl.spi.formatters.DateFormatter
	 */
    public void setFormatter(Formatter<Object> formatter) {
		this.formatter = formatter;
	}

	/**
	 * httl.properties: resolver=httl.spi.resolvers.EngineResolver
	 */
    public void setResolver(Resolver resolver) {
		this.resolver = resolver;
	}

    /**
	 * httl.properties: resolver=httl.spi.loggers.Log4jLogger
	 */
    public void setLogger(Logger logger) {
		this.logger = logger;
	}

    /**
     * httl.properties: engine=httl.spi.engines.DefaultEngine
     */
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    /**
     * httl.properties: time.zone=+8
     */
    public void setTimeZone(String timeZone) {
    	this.timeZone = TimeZone.getTimeZone(timeZone);
    }

    /**
     * httl.properties: date.format=yyyy-MM-dd HH:mm:ss
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
	 * httl.properties: number.format=###,##0.###
	 */
    public void setNumberFormat(String numberFormat) {
        this.numberFormat = numberFormat;
    }

    /**
	 * httl.properties: output.encoding=UTF-8
	 */
    public void setOutputEncoding(String outputEncoding) {
		this.outputEncoding = outputEncoding;
		this.outputCharset = Charset.forName(outputEncoding);
	}

    /**
	 * httl.properties: import.packages=java.util
	 */
    public void setImportPackages(String[] importPackages) {
        this.importPackages = importPackages;
    }

	public String getLocale() {
		return resolver.getProperty("locale");
	}

    public static Date now() {
        return new Date();
    }

    public static int random() {
        return RANDOM.nextInt();
    }

    public static UUID uuid() {
        return UUID.randomUUID();
    }

    public Object include(String name) throws IOException, ParseException {
        return include(name, null, null);
    }

    public Object include(String name, String encoding) throws IOException, ParseException {
    	return include(name, encoding, null);
    }

    public Object include(String name, Map<String, Object> parameters) throws IOException, ParseException {
    	return include(name, null, parameters);
    }

    public Object include(String name, String encoding, Map<String, Object> parameters) throws IOException, ParseException {
        Template template = parse(name, encoding);
        Map<String, Object> map = Context.getContext().getParameters();
        if (parameters != null) {
        	map = new WrappedMap<String, Object>(map, parameters);
        }
        return template.evaluate(map);
    }

    public String locale(String name) {
    	return locale(name, getLocale());
    }

    public String locale(String name, Locale locale) {
    	return locale(name, locale == null ? getLocale() : locale.toString());
    }

    public String locale(String name, String locale) {
    	if (name != null && name.length() > 0
    			&& locale != null && locale.length() > 0) {
    		int i = name.lastIndexOf('.');
    		String prefix;
    		String suffix;
    		if (i > 0) {
    			prefix = name.substring(0, i);
    			suffix = name.substring(i);
    		} else {
    			prefix = name;
    			suffix = "";
    		}
	    	for (;;) {
	    		String path = prefix + "_" + locale + suffix;
	        	if (engine.hasResource(path)) {
	        		return path;
	        	}
	        	int j = locale.lastIndexOf('_');
	        	if (j > 0) {
	        		locale = locale.substring(0, j);
	        	} else {
	        		break;
	        	}
	    	}
    	}
        return name;
    }

    public String i18n(String key) {
    	return i18n(key, null, new String[0]);
    }

    public String i18n(String key, Object arg0) {
    	return i18n(key, null, new Object[] {arg0});
    }

    public String i18n(String key, Object arg0, Object arg1) {
    	return i18n(key, null, new Object[] {arg0, arg1});
    }

    public String i18n(String key, Object arg0, Object arg1, Object arg2) {
    	return i18n(key, null, new Object[] {arg0, arg1, arg2});
    }

    public String i18n(String key, Object arg0, Object arg1, Object arg2, Object arg3) {
    	return i18n(key, null, new Object[] {arg0, arg1, arg2, arg3});
    }

    public String i18n(String key, Object[] args) {
    	return i18n(key, null, args);
    }

    public String i18n(String key, Locale locale) {
    	return i18n(key, locale, new String[0]);
    }

    public String i18n(String key, Locale locale, Object arg0) {
    	return i18n(key, locale, new Object[] {arg0});
    }

    public String i18n(String key, Locale locale, Object arg0, Object arg1) {
    	return i18n(key, locale, new Object[] {arg0, arg1});
    }

    public String i18n(String key, Locale locale, Object arg0, Object arg1, Object arg2) {
    	return i18n(key, locale, new Object[] {arg0, arg1, arg2});
    }

    public String i18n(String key, Locale locale, Object arg0, Object arg1, Object arg2, Object arg3) {
    	return i18n(key, locale, new Object[] {arg0, arg1, arg2, arg3});
    }

    private final ConcurrentMap<String, EncodingProperties> i18nCache = new ConcurrentHashMap<String, EncodingProperties>();

    private String findI18nByLocale(String locale, String key) {
    	String file = i18nBasename + locale + ".properties";
    	EncodingProperties properties = i18nCache.get(file);
		if ((properties == null || reloadable) && engine.hasResource(file)) {
			if (properties == null) {
				properties = new EncodingProperties();
				EncodingProperties old = i18nCache.putIfAbsent(file, properties);
				if (old != null) {
					properties = old;
				}
			}
			try {
				Resource resource = engine.getResource(file);
				if (properties.getLastModified() < resource.getLastModified()) {
					String encoding = (i18nEncoding == null || i18nEncoding.length() == 0 ? "UTF-8" : i18nEncoding);
					properties.load(resource.getInputStream(), encoding, resource.getLastModified());
				}
			} catch (IOException e) {
				if (logger != null && logger.isErrorEnabled()) {
					logger.error("Failed to load httl i18n message file " + file + ", cause: " + e.getMessage(), e);
				}
			}
		}
		if (properties != null) {
			String value = properties.getProperty(key);
			if (value != null && value.length() > 0) {
				return value;
			}
		}
    	if (locale.length() > 0) {
	    	int i = locale.lastIndexOf('_');
			if (i >= 0) {
				return findI18nByLocale(locale.substring(0, i), key);
			}
    	}
		return null;
    }

    public String i18n(String key, Locale locale, Object[] args) {
    	if (i18nBasename == null) {
    		return key;
    	}
    	String localeStr;
		if (locale != null) {
			localeStr = "_" + locale.toString();
		} else {
			localeStr = getLocale();
			if (localeStr == null) {
				localeStr = "";
			} else {
				localeStr = "_" + localeStr;
			}
		}
		String value = findI18nByLocale(localeStr, key);
		if (value != null && value.length() > 0) {
			if (args != null && args.length > 0) {
				if ("string".equals(i18nFormat)) {
					return String.format(value, args);
				} else {
					return MessageFormat.format(value, args);
				}
			} else {
				return value;
			}
		}
		return key;
    }
    
    public static Locale toLocale(String name) {
    	return LocaleUtils.getLocale(name);
    }

    public Object read(String name) throws IOException, ParseException {
        return read(name, null);
    }

    public Object read(String name, String encoding) throws IOException {
        Resource resource = load(name, encoding);
        if (Context.getContext().getOutput() instanceof OutputStream) {
        	return IOUtils.readToBytes(resource.getInputStream());
        } else {
        	return IOUtils.readToString(resource.getReader());
        }
    }

    public Object evaluate(Object source) throws IOException, ParseException {
    	if (source instanceof byte[]) {
    		return evaluate((byte[]) source);
    	}
    	return evaluate((String) source);
    }

    public Object evaluate(byte[] source) throws IOException, ParseException {
    	return evaluate(outputCharset == null ? new String(source) : new String(source, outputCharset));
    }

    public Object evaluate(String expr) throws ParseException {
        return translate(expr).evaluate(Context.getContext().getParameters());
    }

    public Object render(Object source) throws IOException, ParseException {
    	if (source instanceof byte[]) {
    		return render((byte[]) source);
    	}
    	return render((String) source);
    }

    public Object render(byte[] source) throws IOException, ParseException {
    	return render(outputCharset == null ? new String(source) : new String(source, outputCharset));
    }

    public Object render(String source) throws IOException, ParseException {
        Template template = Context.getContext().getTemplate();
        if (template == null) {
            throw new IllegalArgumentException("display context template == null");
        }
        String name = template.getName() + "$" + MD5.getMD5(source);
        if (! engine.hasResource(name)) {
        	engine.addResource(name, source);
        }
        return engine.getTemplate(name).evaluate(Context.getContext().getParameters());
    }

    public Template parse(String name) throws IOException, ParseException {
        return parse(name, null);
    }

    public Template parse(String name, String encoding) throws IOException, ParseException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("include template name == null");
        }
        String macro = null;
		int i = name.indexOf('#');
        if (i > 0) {
        	macro = name.substring(i + 1);
        	name = name.substring(0, i);
        }
        Template template = Context.getContext().getTemplate();
        if (template != null) {
            if (encoding == null || encoding.length() == 0) {
                encoding = template.getEncoding();
            }
            name = UrlUtils.relativeUrl(name, template.getName());
        }
        template = engine.getTemplate(name, encoding);
        if (macro != null && macro.length() > 0) {
			return template.getMacros().get(macro);
		}
        return template;
    }

    public Resource load(String name) throws IOException, ParseException {
        return load(name, null);
    }

    public Resource load(String name, String encoding) throws IOException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("display template name == null");
        }
        Template template = Context.getContext().getTemplate();
        if (template != null) {
            if (encoding == null || encoding.length() == 0) {
                encoding = template.getEncoding();
            }
            name = UrlUtils.relativeUrl(name, template.getName());
        }
        return engine.getResource(name, encoding);
    }

    public Expression translate(String expr) throws ParseException {
    	Template template = Context.getContext().getTemplate();
        if (template == null) {
            throw new IllegalArgumentException("display context template == null");
        }
    	return engine.getExpression(expr, template.getParameterTypes());
    }

    public static String escapeString(String value) {
        return StringUtils.escapeString(value);
    }

    public static String unescapeString(String value) {
        return StringUtils.unescapeString(value);
    }

    public static String escapeHtml(String value) {
        return StringUtils.escapeHtml(value);
    }

    public static String unescapeHtml(String value) {
        return StringUtils.unescapeHtml(value);
    }

    public static String escapeXml(String value) {
        return StringUtils.escapeXml(value);
    }

    public static String unescapeXml(String value) {
        return StringUtils.unescapeXml(value);
    }

    public String escapeUrl(String value) {
    	return escapeUrl(value, outputEncoding);
    }

    public static String escapeUrl(String value, String encoding) {
        try {
			return value == null ? null : URLEncoder.encode(value, encoding);
		} catch (UnsupportedEncodingException e) {
			return value;
		}
    }

    public String unescapeUrl(String value) {
    	return unescapeUrl(value, outputEncoding);
    }

    public static String unescapeUrl(String value, String encoding) {
        try {
			return value == null ? null : URLDecoder.decode(value, encoding);
		} catch (UnsupportedEncodingException e) {
			return value;
		}
    }

    public static boolean toBoolean(Object value) {
    	if (value instanceof Boolean) {
    		return (Boolean) value;
    	}
        return value == null ? false : toBoolean(String.valueOf(value));
    }

    public static char toChar(Object value) {
    	if (value instanceof Character) {
    		return (Character) value;
    	}
        return value == null ? '\0' : toChar(String.valueOf(value));
    }

    public static byte toByte(Object value) {
    	if (value instanceof Number) {
    		return ((Number) value).byteValue();
    	}
        return value == null ? 0 : toByte(String.valueOf(value));
    }

    public static short toShort(Object value) {
    	if (value instanceof Number) {
    		return ((Number) value).shortValue();
    	}
        return value == null ? 0 : toShort(String.valueOf(value));
    }

    public static int toInt(Object value) {
    	if (value instanceof Number) {
    		return ((Number) value).intValue();
    	}
        return value == null ? 0 : toInt(String.valueOf(value));
    }

    public static long toLong(Object value) {
    	if (value instanceof Number) {
    		return ((Number) value).longValue();
    	}
        return value == null ? 0 : toLong(String.valueOf(value));
    }

    public static float toFloat(Object value) {
    	if (value instanceof Number) {
    		return ((Number) value).floatValue();
    	}
        return value == null ? 0 : toFloat(String.valueOf(value));
    }

    public static double toDouble(Object value) {
    	if (value instanceof Number) {
    		return ((Number) value).doubleValue();
    	}
        return value == null ? 0 : toDouble(String.valueOf(value));
    }

    public static Class<?> toClass(Object value) {
    	if (value instanceof Class) {
    		return (Class<?>) value;
    	}
        return value == null ? null : toClass(String.valueOf(value));
    }

    public static boolean toBoolean(String value) {
        return value == null || value.length() == 0 ? false : Boolean.parseBoolean(value);
    }

    public static char toChar(String value) {
        return value == null || value.length() == 0 ? '\0' : value.charAt(0);
    }

    public static byte toByte(String value) {
        return value == null || value.length() == 0 ? 0 : Byte.parseByte(value);
    }

    public static short toShort(String value) {
        return value == null || value.length() == 0 ? 0 : Short.parseShort(value);
    }

    public static int toInt(String value) {
        return value == null || value.length() == 0 ? 0 : Integer.parseInt(value);
    }

    public static long toLong(String value) {
        return value == null || value.length() == 0 ? 0 : Long.parseLong(value);
    }

    public static float toFloat(String value) {
        return value == null || value.length() == 0 ? 0 : Float.parseFloat(value);
    }

    public static double toDouble(String value) {
        return value == null || value.length() == 0 ? 0 : Double.parseDouble(value);
    }

    public static Class<?> toClass(String value) {
        return value == null || value.length() == 0 ? null : ClassUtils.forName(value);
    }

    @SuppressWarnings("unchecked")
	public <T> T[] toArray(Collection<T> value, String type) {
        Class<T> cls = (Class<T>) ClassUtils.forName(importPackages, type);
        if (value == null) {
        	return (T[]) Array.newInstance(cls, 0);
        }
        return (T[]) value.toArray((Object[])Array.newInstance(cls, value.size()));
    }

    public Date toDate(String value) {
        try {
            return value == null || value.length() == 0 ? null : DateUtils.parse(value, dateFormat, timeZone);
        } catch (Exception e) {
            try {
                return DateUtils.parse(value, "yyyy-MM-dd");
            } catch (Exception e2) {
                return DateUtils.parse(value, "yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    public Date toDate(String value, String format) {
        return value == null || value.length() == 0 ? null : DateUtils.parse(value, format, timeZone);
    }

    public static Date toDate(String value, String format, String timeZone) {
        return value == null || value.length() == 0 ? null : DateUtils.parse(value, format, timeZone == null ? null : TimeZone.getTimeZone(timeZone));
    }

    public String toString(Date value) {
        return value == null ? null : DateUtils.format(value, dateFormat, timeZone);
    }

    public String format(Date value, String format) {
        return value == null ? null : DateUtils.format(value, format, timeZone);
    }

    public static String format(Date value, String format, String timeZone) {
        return value == null ? null : DateUtils.format(value, format, timeZone == null ? null : TimeZone.getTimeZone(timeZone));
    }

    public static String toString(boolean value) {
        return String.valueOf(value);
    }

    public static String toString(char value) {
        return String.valueOf(value);
    }

    public String toString(byte value) {
        return format(Byte.valueOf(value), numberFormat);
    }

    public String toString(short value) {
        return format(Short.valueOf(value), numberFormat);
    }

    public String toString(int value) {
        return format(Integer.valueOf(value), numberFormat);
    }

    public String toString(long value) {
        return format(Long.valueOf(value), numberFormat);
    }

    public String toString(float value) {
        return format(Float.valueOf(value), numberFormat);
    }

    public String toString(double value) {
        return format(Double.valueOf(value), numberFormat);
    }

    public String toString(Number value) {
        return format(value, numberFormat);
    }

    public String toString(byte[] value) {
    	return value == null ? null : (outputCharset == null 
    			? new String(value) : new String(value, outputCharset));
    }

    public String toString(Object value) {
    	if (value == null)
            return null;
    	if (value instanceof String)
            return (String) value;
    	if (value instanceof Number)
    		return toString((Number) value);
    	if (value instanceof Date)
    		return toString((Date) value);
    	if (value instanceof byte[])
    		return toString((byte[]) value);
        if (formatter != null)
            return formatter.format(value);
    	return StringUtils.toString(value);
    }

    public static String format(byte value, String format) {
        return format(Byte.valueOf(value), format);
    }

    public static String format(short value, String format) {
        return format(Short.valueOf(value), format);
    }

    public static String format(int value, String format) {
        return format(Integer.valueOf(value), format);
    }

    public static String format(long value, String format) {
        return format(Long.valueOf(value), format);
    }

    public static String format(float value, String format) {
        return format(Float.valueOf(value), format);
    }

    public static String format(double value, String format) {
        return format(Double.valueOf(value), format);
    }

    public static String format(Number value, String format) {
        return value == null ? null : NumberUtils.format(value, format);
    }

    public static <T> ListCycle<T> toCycle(Collection<T> values) {
        return new ListCycle<T>(values);
    }

    public static <T> ArrayCycle<T> toCycle(T[] values) {
        return new ArrayCycle<T>(values);
    }

    public static BooleanArrayCycle toCycle(boolean[] values) {
        return new BooleanArrayCycle(values);
    }

    public static CharArrayCycle toCycle(char[] values) {
        return new CharArrayCycle(values);
    }

    public static ByteArrayCycle toCycle(byte[] values) {
        return new ByteArrayCycle(values);
    }

    public static ShortArrayCycle toCycle(short[] values) {
        return new ShortArrayCycle(values);
    }

    public static IntArrayCycle toCycle(int[] values) {
        return new IntArrayCycle(values);
    }

    public static LongArrayCycle toCycle(long[] values) {
        return new LongArrayCycle(values);
    }

    public static FloatArrayCycle toCycle(float[] values) {
        return new FloatArrayCycle(values);
    }

    public static DoubleArrayCycle toCycle(double[] values) {
        return new DoubleArrayCycle(values);
    }

    public static int length(Map<?, ?> values) {
        return values == null ? 0 : values.size();
    }

    public static int length(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    public static int length(Object[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(boolean[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(char[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(byte[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(short[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(int[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(long[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(float[] values) {
        return values == null ? 0 : values.length;
    }

    public static int length(double[] values) {
        return values == null ? 0 : values.length;
    }

    public static String repeat(String value, int count) {
        if (value == null || value.length() == 0 || count <= 0) {
            return value;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < count; i ++) {
            buf.append(value);
        }
        return buf.toString();
    }

	public static String[] split(String value, char separator) {
		if (value == null || value.length() == 0) {
			return new String[0];
		}
		List<String> list = new ArrayList<String>();
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < value.length(); i ++) {
			char ch = value.charAt(i);
			if (ch == separator) {
				if (buf.length() > 0) {
					list.add(buf.toString());
					buf.setLength(0);
				}
			} else {
				buf.append(ch);
			}
		}
		if (buf.length() > 0) {
			list.add(buf.toString());
		}
		return list.toArray(new String[list.size()]);
	}

	public static Map<String, Object> parseJson(String json) {
		if (json == null) {
			return null;
		}
		return JSON.parseObject(json);
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseJson(String json, Class<T> cls) {
		if (json == null) {
			return null;
		}
		if (cls == null) {
			return (T) JSON.parseObject(json);
		}
		return JSON.parseObject(json, cls);
	}

	public static String toJson(Object object) {
		if (object == null) {
			return null;
		}
		return JSON.toJSONString(object);
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseXstream(String xml) {
		if (xml == null) {
			return null;
		}
		return (T) new XStream(new DomDriver()).fromXML(xml);
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseXstream(String xml, Class<T> cls) {
		if (xml == null) {
			return null;
		}
		if (cls == null) {
			return (T) new XStream(new DomDriver()).fromXML(xml);
		}
		try {
			return (T) new XStream(new DomDriver()).fromXML(xml, cls.newInstance());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static String toXstream(Object object) {
		return new XStream(new DomDriver()).toXML(object);
	}

	public static Object parseXbean(String xml) {
		if (xml == null) {
			return null;
		}
		ByteArrayInputStream bi = new ByteArrayInputStream(xml.getBytes());
        XMLDecoder xd = new XMLDecoder(bi);
        return xd.readObject();
	}
	
	public static String toXbean(Object object) {
		if (object == null) {
			return null;
		}
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
        XMLEncoder xe = new XMLEncoder(bo);
        try {
            xe.writeObject(object);
            xe.flush();
        } finally {
            xe.close();
        }
        return new String(bo.toByteArray());
	}
	
	public static <K, V> Map<K, V> sort(Map<K, V> map) {
		if (map == null) {
			return null;
		}
		return new TreeMap<K, V>(map);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<T> sort(List<T> list) {
		if (list == null) {
			return null;
		}
		list = new ArrayList<T>(list);
		Collections.sort((List) list);
		return list;
	}

	public static <T> Set<T> sort(Set<T> set) {
		if (set == null) {
			return null;
		}
		return new TreeSet<T>(set);
	}
	
	public static <T> Collection<T> sort(Collection<T> set) {
		if (set == null) {
			return null;
		}
		return new TreeSet<T>(set);
	}

	public static <T> T[] sort(T[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static char[] sort(char[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static byte[] sort(byte[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static short[] sort(short[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static int[] sort(int[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static long[] sort(long[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static float[] sort(float[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static double[] sort(double[] array) {
		if (array == null) {
			return null;
		}
		array = Arrays.copyOf(array, array.length);
		Arrays.sort(array);
		return array;
	}

	public static String toUnderlineName(String name) {
		if (name == null || name.length() == 0) {
    		return name;
    	}
    	StringBuilder buf = new StringBuilder(name.length() * 2);
    	buf.append(Character.toLowerCase(name.charAt(0)));
    	for (int i = 1; i < name.length(); i ++) {
    		char c = name.charAt(i);
    		if (c >= 'A' && c <= 'Z') {
    			buf.append('_');
    			buf.append(Character.toLowerCase(c));
    		} else {
    			buf.append(c);
    		}
    	}
    	return buf.toString();
	}

    public static String toCamelName(String name) {
    	if (name == null || name.length() == 0) {
    		return name;
    	}
    	StringBuilder buf = new StringBuilder(name.length());
    	boolean upper = false;
    	for (int i = 0; i < name.length(); i ++) {
    		char c = name.charAt(i);
    		if (c == '_') {
    			upper = true;
    		} else {
    			if (upper) {
    				upper = false;
    				c = Character.toUpperCase(c);
    			}
    			buf.append(c);
    		}
    	}
    	return buf.toString();
    }

    public static String toCapitalName(String name) {
    	if (name == null || name.length() == 0) {
    		return name;
    	}
    	StringBuilder buf = new StringBuilder(name.length());
    	boolean upper = true;
    	for (int i = 0; i < name.length(); i ++) {
    		char c = name.charAt(i);
    		if (c == '_') {
    			upper = true;
    		} else {
    			if (upper) {
    				upper = false;
    				c = Character.toUpperCase(c);
    			}
    			buf.append(c);
    		}
    	}
    	return buf.toString();
    }

}
