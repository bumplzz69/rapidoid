package org.rapidoid.app;

/*
 * #%L
 * rapidoid-app
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Map;
import java.util.Map.Entry;

import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.beany.BeanProperties;
import org.rapidoid.beany.Beany;
import org.rapidoid.beany.Prop;
import org.rapidoid.config.Conf;
import org.rapidoid.http.HttpExchangeImpl;
import org.rapidoid.io.Res;
import org.rapidoid.util.U;
import org.rapidoid.webapp.AppCtx;

@Authors("Nikolche Mihajlovski")
@Since("4.2.0")
public class Scripting {

	public static Object runDynamicScript(HttpExchangeImpl x) {
		String scriptName = x.isGetReq() ? x.resourceName() : x.verb().toUpperCase() + "_" + x.resourceName();
		String filename = scriptName + ".js";
		String firstFile = Conf.dynamicPath() + "/" + filename;
		String defaultFile = Conf.dynamicPathDefault() + "/" + filename;
		Res res = Res.from(filename, true, firstFile, defaultFile);

		if (!res.exists()) {
			return null;
		}

		String js = res.getContent();
		CompiledScript compiled;
		try {
			compiled = U.compileJS(js);
		} catch (ScriptException e) {
			throw U.rte("Script compilation error!", e);
		}

		Map<String, Object> bindings = U.map();

		for (Entry<String, String> e : x.data().entrySet()) {
			bindings.put("$" + e.getKey(), e.getValue());
		}

		Dollar dollar = new Dollar(x, bindings);

		bindings.put("$", dollar);

		try {
			return compiled.eval(new SimpleBindings(bindings));
		} catch (ScriptException e) {
			throw U.rte("Script execution error!", e);
		}
	}

	public static Object desc(HttpExchangeImpl x) {
		Map<String, Object> desc = U.map();

		desc.put("verb", x.verb());
		desc.put("uri", x.uri());
		desc.put("path", x.path());
		desc.put("home", x.home());
		desc.put("dev", x.isDevMode());

		boolean loggedIn = AppCtx.isLoggedIn();
		desc.put("loggedIn", loggedIn);
		desc.put("user", loggedIn ? AppCtx.user() : null);

		return GUI.multi(GUI.h2("Request details:"), GUI.grid(desc), GUI.h2("Request params:"), GUI.grid(x.data()),
				GUI.h2("Cookies:"), GUI.grid(x.cookies()));
	}

	public static Object desc(Dollar dollar) {
		Map<String, Object> desc = U.map();
		BeanProperties props = Beany.propertiesOf(dollar);

		for (Prop prop : props) {
			Object val = prop.get(dollar);
			desc.put(prop.getName(), val.getClass().getSimpleName());
		}

		return GUI.multi(GUI.h2("The $ properties:"), GUI.grid(desc), GUI.h2("Bindings:"), GUI.grid(dollar.bindings));
	}

}
