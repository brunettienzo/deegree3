//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.maven;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.deegree.commons.utils.io.Zip.unzip;
import static org.deegree.maven.utils.ClasspathHelper.getDependencyArtifacts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * @goal workspace-inplace
 * @phase generate-resources
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class WorkspaceInplaceMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * 
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * 
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * If set to true, all existing files will be overwritten if contained in a dependency.
     * 
     * @parameter default-value="false"
     * @required
     * @readonly
     */
    private boolean overwrite;

    public void execute()
                            throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        File dir = new File( project.getBasedir(), "src/main/webapp/WEB-INF/workspace" );
        if ( !dir.isDirectory() ) {
            dir = new File( project.getBasedir(), "src/main/webapp/WEB-INF/conf" );
        }
        if ( !dir.isDirectory() ) {
            dir = new File( project.getBasedir(), "src/main/workspace" );
        }

        try {
            Set<?> workspaces = getDependencyArtifacts( project, artifactResolver, artifactFactory, metadataSource,
                                                        localRepository, "deegree-workspace" );

            for ( Object o : workspaces ) {
                Artifact a = (Artifact) o;
                log.info( "Unpacking workspace " + a.getArtifactId() );
                FileInputStream in = new FileInputStream( a.getFile() );
                try {
                    unzip( in, dir, overwrite );
                } finally {
                    closeQuietly( in );
                }
            }
        } catch ( ArtifactResolutionException e1 ) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch ( ArtifactNotFoundException e1 ) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch ( InvalidDependencyVersionException e1 ) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch ( IOException e ) {
            log.debug( e );
            throw new MojoFailureException( "Could not extract workspace dependencies in place: "
                                            + e.getLocalizedMessage() );
        }

    }

}
