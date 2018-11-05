package id.tokoonderdil.study.springbootcamel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;

@SpringBootApplication(scanBasePackages = "id.tokoonderdil.study.springbootcamel")
public class SpringbootcamelApplication {

    @Value("${server.port}")
    String serverPort;

    @Value("${tokoonderdil.api.path}")
    String contextPath;

    public static void main(String[] args) {
        SpringApplication.run(SpringbootcamelApplication.class, args);
    }

    @Bean
    ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new CamelHttpTransportServlet(), contextPath + "/*");
        servletRegistrationBean.setName("CamelServlet");
        return servletRegistrationBean;
    }

    @Component
    class RestApi extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            CamelContext camelContext = new DefaultCamelContext();

            // camel/api-doc
            restConfiguration().contextPath(contextPath)
                    .port(serverPort)
                    .enableCORS(true)
                    .apiContextPath("/api-doc")
                    .apiProperty("api.title", "TEST REST API")
                    .apiProperty("api.version", "v1")
                    .apiProperty("cors", "true")
                    .apiContextRouteId("doc-api")
                    .component("servlet")
                    .bindingMode(RestBindingMode.json)
                    .dataFormatProperty("prettyPrint", "true");

            rest("/api/").description("TEST REST SERVICE")
                    .id("api-route")
                    .post("/bean")
                    //.get("/hello/{place}")
                    .produces(MediaType.APPLICATION_JSON)
                    .consumes(MediaType.APPLICATION_JSON)
                    .bindingMode(RestBindingMode.auto)
                    .type(MyBean.class)
                    .enableCORS(true)
                    //.outType(OutBean.class)
                    .to("direct:remoteService");

            from("direct:remoteService")
                    .routeId("direct-route")
                    .tracing()
                    .log(">>> ${body.id}")
                    .log(">>> ${body.name}")
                    //.transform().simple("Blue $(in.body.name)")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            MyBean bodyIn = (MyBean) exchange.getIn().getBody();
                            ExampleServices.example(bodyIn);
                            exchange.getIn().setBody(bodyIn);
                        }
                    })
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));
        }
    }
}
