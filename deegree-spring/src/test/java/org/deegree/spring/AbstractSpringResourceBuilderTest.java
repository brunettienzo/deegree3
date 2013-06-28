package org.deegree.spring;

import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceInitException;
import org.deegree.workspace.ResourceMetadata;
import org.deegree.workspace.Workspace;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractSpringResourceBuilderTest {

    static abstract class TestResource implements Resource {

        @Override
        public void destroy() {

        }

        @Override
        @SuppressWarnings("unchecked")
        public ResourceMetadata<? extends Resource> getMetadata() {
            return mock( ResourceMetadata.class );
        }

        @Override
        public void init() {

        }
    }

    static class NoBean extends TestResource {
    }

    static class SingleBean extends TestResource {
    }

    static class ContentBean extends TestResource {

        final String content;

        ContentBean( final String content ) {
            this.content = content;
        }

        String getContent() {
            return content;
        }
    }

    @Configuration
    static class Config {

        @Bean
        public SingleBean singleBean() {
            return new SingleBean();
        }

        @Bean
        public ContentBean contentBean0() {
            return new ContentBean( "contentBean0" );
        }

        @Bean
        public ContentBean contentBean1() {
            return new ContentBean( "contentBean1" );
        }

        @Bean
        public ContentBean contentBean() {
            return new ContentBean( "contentBean" );
        }
    }

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext( Config.class );

    ApplicationContextHolder contextHolder = new ApplicationContextHolder( null, context );

    Workspace workspace;

    @Before
    public void setUp() {
        workspace = mock( Workspace.class );
        when( workspace.getResource( ApplicationContextHolderProvider.class, "test" ) ).thenReturn( contextHolder );
    }

    @Test(expected = ResourceInitException.class)
    public void testGetNonExistingBeanType() {
        final AbstractSpringResourceBuilder<NoBean> builder = new AbstractSpringResourceBuilder<NoBean>(
                                                                                                                 workspace,
                                                                                                                 "test" ) {

            @Override
            public NoBean build() {
                return getBean( NoBean.class );
            }
        };
        
        builder.build();
    }
    
    @Test(expected = ResourceInitException.class)
    public void testGetNonExistingBeanName() {
        final AbstractSpringResourceBuilder<SingleBean> builder = new AbstractSpringResourceBuilder<SingleBean>(
                                                                                                                 workspace,
                                                                                                                 "test" ) {

            @Override
            public SingleBean build() {
                return getBean( SingleBean.class, "wrongName" );
            }
        };
        
        builder.build();
    }

    @Test
    public void testGetSingleBean() {
        final AbstractSpringResourceBuilder<SingleBean> builder = new AbstractSpringResourceBuilder<SingleBean>(
                                                                                                                 workspace,
                                                                                                                 "test" ) {

            @Override
            public SingleBean build() {
                return getBean( SingleBean.class );
            }
        };

        assertEquals( context.getBean( SingleBean.class ), builder.build() );
    }

    @Test(expected = ResourceInitException.class)
    public void testGetBeanNameMissing() {
        final AbstractSpringResourceBuilder<ContentBean> builder = new AbstractSpringResourceBuilder<ContentBean>(
                                                                                                                   workspace,
                                                                                                                   "test" ) {

            @Override
            public ContentBean build() {
                return getBean( ContentBean.class );
            }
        };

        builder.build();
    }

    @Test
    public void testGetNamedBean() {
        final AbstractSpringResourceBuilder<ContentBean> builder = new AbstractSpringResourceBuilder<ContentBean>(
                                                                                                                   workspace,
                                                                                                                   "test" ) {

            @Override
            public ContentBean build() {
                return getBean( ContentBean.class, "contentBean0" );
            }
        };

        final ContentBean bean = builder.build();
        assertNotNull( bean );
        assertEquals( "contentBean0", bean.getContent() );
    }

    @Test
    public void testGetConventionallyNamedBean() {
        final AbstractSpringResourceBuilder<ContentBean> builder = new AbstractSpringResourceBuilder<ContentBean>(
                                                                                                                   workspace,
                                                                                                                   "test" ) {

            @Override
            public ContentBean build() {
                return getBean( ContentBean.class, null, "contentBean" );
            }
        };

        final ContentBean bean = builder.build();
        assertNotNull( bean );
        assertEquals( "contentBean", bean.getContent() );
    }
    
    @Test(expected = ResourceInitException.class)
    public void testGetNonConventionallyNamedBean() {
        final AbstractSpringResourceBuilder<ContentBean> builder = new AbstractSpringResourceBuilder<ContentBean>(
                                                                                                                   workspace,
                                                                                                                   "test" ) {

            @Override
            public ContentBean build() {
                return getBean( ContentBean.class, null, "defaultContentBean" );
            }
        };

        builder.build();       
    }
}
