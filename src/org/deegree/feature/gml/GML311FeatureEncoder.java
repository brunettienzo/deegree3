//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/deegree3/commons/trunk/src/org/deegree/model/feature/Feature.java $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

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

package org.deegree.feature.gml;

import static org.deegree.commons.xml.CommonNamespaces.GMLNS;
import static org.deegree.commons.xml.CommonNamespaces.XLNNS;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.deegree.commons.types.gml.StandardObjectProps;
import org.deegree.commons.types.ows.CodeType;
import org.deegree.commons.types.ows.StringOrRef;
import org.deegree.commons.uom.Length;
import org.deegree.commons.uom.Measure;
import org.deegree.feature.Feature;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.GenericFeatureCollection;
import org.deegree.feature.Property;
import org.deegree.feature.types.LengthPropertyType;
import org.deegree.feature.types.property.CodePropertyType;
import org.deegree.feature.types.property.EnvelopePropertyType;
import org.deegree.feature.types.property.FeaturePropertyType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.MeasurePropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.deegree.filter.expression.PropertyName;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.gml.GML311GeometryEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes features and properties into GML. Delegates {@link Geometry} exporting tasks to the
 * {@link GML311GeometryEncoder}.
 * <p>
 * <h4>XLink handling</h4>
 * This implementation is aware of xlinks (local as well as remote) and allows to set the
 * <code>traverseXlinkDepth</code> parameter, which controls the number of feature levels that are exported inline.
 * </p>
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author <a href="mailto:ionita@lat-lon.de">Andrei Ionita</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class GML311FeatureEncoder {

    private static final Logger LOG = LoggerFactory.getLogger( GML311FeatureEncoder.class );

    private Set<String> exportedIds = new HashSet<String>();

    private XMLStreamWriter writer;

    private GML311GeometryEncoder geometryExporter;

    private String referenceTemplate;

    private Set<PropertyName> propNames = new HashSet<PropertyName>();

    // export all levels by default
    private int traverseXlinkDepth = -1;

    private int traverseXlinkExpiry;

    /**
     * @param writer
     */
    public GML311FeatureEncoder( XMLStreamWriter writer ) {
        this.writer = writer;
        geometryExporter = new GML311GeometryEncoder( writer, exportedIds );
    }

    /**
     * @param writer
     * @param referenceTemplate
     *            URI template used to create references to local objects, e.g.
     *            <code>http://localhost:8080/d3_wfs_lab/services?SERVICE=WFS&REQUEST=GetGmlObject&VERSION=1.1.0&TRAVERSEXLINKDEPTH=1&GMLOBJECTID={}</code>
     *            , the substring <code>{}</code> is replaced by the object id
     * @param requestedProps
     *            properties to be exported, may be <code>null</code>
     * @param traverseXlinkDepth
     * @param traverseXlinkExpiry
     */
    public GML311FeatureEncoder( XMLStreamWriter writer, String referenceTemplate, PropertyName[] requestedProps,
                                 int traverseXlinkDepth, int traverseXlinkExpiry ) {
        this.writer = writer;
        this.referenceTemplate = referenceTemplate;
        if ( requestedProps != null ) {
            for ( PropertyName propertyName : requestedProps ) {
                this.propNames.add( propertyName );
            }
        }
        this.traverseXlinkDepth = traverseXlinkDepth;
        this.traverseXlinkExpiry = traverseXlinkExpiry;
        geometryExporter = new GML311GeometryEncoder( writer, exportedIds );
    }

    public void export( Feature feature )
                            throws XMLStreamException {
        export( feature, 0 );
    }

    public void export( FeatureCollection fc, QName name )
                            throws XMLStreamException {
        LOG.debug( "Exporting feature collection with explicit name." );
        writer.setPrefix( "gml", GMLNS );
        writer.writeStartElement( name.getNamespaceURI(), name.getLocalPart() );
        for ( Feature member : fc ) {
            String memberFid = member.getId();
            writer.writeStartElement( "http://www.opengis.net/gml", "featureMember" );
            if ( memberFid != null && exportedIds.contains( memberFid ) ) {
                writer.writeAttribute( XLNNS, "href", "#" + memberFid );
            } else {
                export( member, 0 );
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void export( Feature feature, int inlineLevels )
                            throws XMLStreamException {

        if ( feature.getId() != null ) {
            exportedIds.add( feature.getId() );
        }
        if ( feature instanceof GenericFeatureCollection ) {
            LOG.debug( "Exporting generic feature collection." );
            writer.setPrefix( "gml", GMLNS );
            writer.writeStartElement( "FeatureCollection" );
            for ( Feature member : ( (FeatureCollection) feature ) ) {
                String memberFid = member.getId();
                writer.writeStartElement( "http://www.opengis.net/gml", "featureMember" );
                if ( memberFid != null && exportedIds.contains( memberFid ) ) {
                    writer.writeAttribute( XLNNS, "href", "#" + memberFid );
                } else {
                    export( member, inlineLevels + 1 );
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } else {
            QName featureName = feature.getName();
            LOG.debug( "Exporting Feature {} with ID {}", featureName, feature.getId() );

            String namespaceURI = featureName.getNamespaceURI();
            String localName = featureName.getLocalPart();
            if ( namespaceURI == null || namespaceURI.length() == 0 ) {
                writer.writeStartElement( localName );
            } else {
                // TODO find a clever strategy for binding of the namespaces
                writer.setPrefix( "app", namespaceURI );
                writer.writeStartElement( namespaceURI, localName );
            }

            // writeStartElementWithNS( featureName.getNamespaceURI(), featureName.getLocalPart() );
            if ( feature.getId() != null ) {
                writer.writeAttribute( "gml", GMLNS, "id", feature.getId() );
            }
            if ( feature.getStandardGMLProperties() != null ) {
                exportStandardProps( writer, feature.getStandardGMLProperties() );
            }
            for ( Property<?> prop : feature.getProperties() ) {
                export( prop, inlineLevels );
            }
            writer.writeEndElement();
        }
    }

    protected void export( Property<?> property, int inlineLevels )
                            throws XMLStreamException {

        QName propName = property.getName();
        PropertyType propertyType = property.getType();
        if ( propertyType.getMinOccurs() == 0 ) {
            LOG.debug( "Optional property '" + propName + "', checking if it is requested." );
            if (!isPropertyRequested( propName )) {
                LOG.debug( "Skipping it." );
                return;
            }
        }

        Object value = property.getValue();
        if ( propertyType instanceof FeaturePropertyType ) {
            Feature subFeature = (Feature) value;
            String subFid = subFeature.getId();
            if ( subFid != null && exportedIds.contains( subFid ) ) {
                writeEmptyElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
                writer.writeAttribute( XLNNS, "href", "#" + subFid );
            } else {
                if ( ( subFeature instanceof FeatureReference ) && !( (FeatureReference) subFeature ).isLocal() ) {
                    writeEmptyElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
                    writer.writeAttribute( XLNNS, "href", ( (FeatureReference) subFeature ).getHref() );
                } else if ( referenceTemplate == null || subFid == null || traverseXlinkDepth == -1
                            || ( traverseXlinkDepth > 0 && ( inlineLevels < traverseXlinkDepth ) ) ) {
                    exportedIds.add( subFeature.getId() );
                    writeStartElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
                    export( subFeature, inlineLevels + 1 );
                    writer.writeEndElement();
                } else {
                    writeEmptyElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
                    String uri = referenceTemplate.replace( "{}", subFid );
                    writer.writeAttribute( XLNNS, "href", uri );
                }
            }
        } else if ( propertyType instanceof SimplePropertyType ) {
            writeStartElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
            writer.writeCharacters( value.toString() );
            writer.writeEndElement();
        } else if ( propertyType instanceof GeometryPropertyType ) {
            Geometry gValue = (Geometry) value;
            if ( gValue.getId() != null && exportedIds.contains( gValue.getId() ) ) {
                writeEmptyElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
                writer.writeAttribute( XLNNS, "href", "#" + gValue.getId() );
            } else {
                exportedIds.add( gValue.getId() );
                writeStartElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
                geometryExporter.export( (Geometry) value );
                writer.writeEndElement();
            }
        } else if ( propertyType instanceof CodePropertyType ) {
            writeStartElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
            CodeType codeType = (CodeType) value;
            if ( codeType.getCodeSpace() != null && codeType.getCodeSpace().length() > 0 )
                writer.writeAttribute( "codeSpace", codeType.getCodeSpace() );
            writer.writeCharacters( codeType.getCode() );
            writer.writeEndElement();
        } else if ( propertyType instanceof EnvelopePropertyType ) {
            geometryExporter.exportEnvelope( (Envelope) value );
        } else if ( propertyType instanceof LengthPropertyType ) {
            Length length = (Length) value;
            writeStartElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
            writer.writeAttribute( "uom", length.getUomUri() );
            writer.writeCharacters( String.valueOf( length.getValue() ) );
            writer.writeEndElement();
        } else if ( propertyType instanceof MeasurePropertyType ) {
            Measure measure = (Measure) value;
            writeStartElementWithNS( propName.getNamespaceURI(), propName.getLocalPart() );
            writer.writeAttribute( "uom", measure.getUomUri() );
            writer.writeCharacters( String.valueOf( measure.getValue() ) );
            writer.writeEndElement();
        }
    }

    private void writeStartElementWithNS( String namespaceURI, String localname )
                            throws XMLStreamException {
        if ( namespaceURI == null || namespaceURI.length() == 0 ) {
            writer.writeStartElement( localname );
        } else {
            writer.writeStartElement( namespaceURI, localname );
        }
    }

    private void writeEmptyElementWithNS( String namespaceURI, String localname )
                            throws XMLStreamException {
        if ( namespaceURI == null || namespaceURI.length() == 0 ) {
            writer.writeEmptyElement( localname );
        } else {
            writer.writeEmptyElement( namespaceURI, localname );
        }
    }

    public boolean isExported( String memberFid ) {
        return exportedIds.contains( memberFid );
    }

    private void exportStandardProps( XMLStreamWriter writer, StandardObjectProps standardGMLProperties )
                            throws XMLStreamException {

        StringOrRef description = standardGMLProperties.getDescription();
        if ( description != null && isPropertyRequested( new QName( GMLNS, "description" ) ) ) {
            writer.writeStartElement( GMLNS, "description" );
            if ( description.getRef() != null ) {
                writer.writeAttribute( XLNNS, "xlink", description.getRef() );
            }
            if ( description.getString() != null ) {
                writer.writeCharacters( description.getString() );
            }
            writer.writeEndElement();
        }

        if ( isPropertyRequested( new QName( GMLNS, "name" ) ) ) {
            for ( CodeType name : standardGMLProperties.getNames() ) {
                writer.writeStartElement( GMLNS, "name" );
                if ( name.getCodeSpace() != null ) {
                    writer.writeAttribute( "codeSpace", name.getCodeSpace() );
                }
                if ( name.getCode() != null ) {
                    writer.writeCharacters( name.getCode() );
                }
                writer.writeEndElement();
            }
        }
    }

    private boolean isPropertyRequested( QName propName ) {
        // TODO compare names properly (different types)
        return propNames.size() == 0 || propNames.contains( propName );
    }
}
