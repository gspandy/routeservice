package c.z.route;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class RouteScannerConfigurer
        implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

    private static final Logger logger = LoggerFactory.getLogger(RouteScannerConfigurer.class);
    private String basePackage;
    private Class<? extends Annotation> annotationClass = RouteService.class;
    private ApplicationContext applicationContext;

    private String beanName;

    private boolean processPropertyPlaceHolders;
    private BeanNameGenerator nameGenerator = new BeanNameGeneratorImpl();

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
        this.processPropertyPlaceHolders = processPropertyPlaceHolders;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        this.applicationContext = applicationContext;
    }

    public void setNameGenerator(BeanNameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }

    public BeanNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // System.out.println("beanFactory.....");
        // do nothing
    
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.basePackage, "Property 'basePackage' is required");

        Assert.notNull(this.annotationClass, "Property 'annotationclass is required'");
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        logger.info("进行bean 扫描注册...");
        if (this.processPropertyPlaceHolders) {
            processPropertyPlaceHolders();
        }
        ClassPathRouteServiceScanner scanner = new ClassPathRouteServiceScanner(registry);
        scanner.setAnnotationClass(this.annotationClass);
        scanner.setApplicationContext(applicationContext);
        scanner.setResourceLoader(applicationContext);
        scanner.setBeanNameGenerator(this.nameGenerator);
        scanner.registerFilters();
        scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

    }

    /*
     * BeanDefinitionRegistries are called early in application startup, before
     * BeanFactoryPostProcessors. This means that PropertyResourceConfigurers
     * will not have been loaded and any property substitution of this class'
     * properties will fail. To avoid this, find any PropertyResourceConfigurers
     * defined in the context and run them on this class' bean definition. Then
     * update the values.
     */
    private void processPropertyPlaceHolders() {
        Map<String, PropertyResourceConfigurer> prcs = applicationContext
                .getBeansOfType(PropertyResourceConfigurer.class);

        if (!prcs.isEmpty() && applicationContext instanceof ConfigurableApplicationContext) {
            BeanDefinition mapperScannerBean = ((ConfigurableApplicationContext) applicationContext).getBeanFactory()
                    .getBeanDefinition(beanName);

            // PropertyResourceConfigurer does not expose any methods to
            // explicitly perform
            // property placeholder substitution. Instead, create a BeanFactory
            // that just
            // contains this mapper scanner and post process the factory.
            DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
            factory.registerBeanDefinition(beanName, mapperScannerBean);

            for (PropertyResourceConfigurer prc : prcs.values()) {
                prc.postProcessBeanFactory(factory);
            }

            PropertyValues values = mapperScannerBean.getPropertyValues();

            this.basePackage = updatePropertyValue("basePackage", values);
        }
    }

    private String updatePropertyValue(String propertyName, PropertyValues values) {
        PropertyValue property = values.getPropertyValue(propertyName);

        if (property == null) {
            return null;
        }

        Object value = property.getValue();

        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return value.toString();
        } else if (value instanceof TypedStringValue) {
            return ((TypedStringValue) value).getValue();
        } else {
            return null;
        }
    }

}