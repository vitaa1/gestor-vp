package io.github.vitaa1.gestorvp.web;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration(proxyBeanMethods = false)
class SpaWebConfiguration implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new SpaPathResourceResolver());
	}

	private static final class SpaPathResourceResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			if (isReservedPath(resourcePath)) {
				return null;
			}

			Resource requestedResource = super.getResource(resourcePath, location);
			if (requestedResource != null) {
				return requestedResource;
			}

			if (resourcePath.contains(".")) {
				return null;
			}

			return super.getResource("index.html", location);
		}

		private boolean isReservedPath(String resourcePath) {
			return resourcePath.equals("api") || resourcePath.startsWith("api/")
					|| resourcePath.equals("actuator") || resourcePath.startsWith("actuator/");
		}
	}
}
