//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth  
 lat/lon GmbH 
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de


 ---------------------------------------------------------------------------*/

package org.deegree.model.generic.implementation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.deegree.model.generic.Attribute;
import org.deegree.model.generic.DeegreeObject;
import org.deegree.model.generic.Node;
import org.deegree.model.generic.implementation.schema.GenericObjectType;
import org.deegree.model.generic.schema.ObjectType;

/**
 * TODO add documentation here
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class GenericDeegreeObject extends GenericNode implements DeegreeObject {

    private List<Attribute> attributes = new ArrayList<Attribute>();    
    
    private List<Node> contents = new ArrayList<Node>();

    public GenericDeegreeObject( GenericObjectType ot ) {
        super (ot);
    }    
    
    public GenericDeegreeObject( ObjectType ot, List<Attribute> attributes, List<Node> contents ) {
        super( ot );
        this.attributes = attributes;
        this.contents = contents;
    }

    @Override
    public ObjectType getType() {
        return (ObjectType) type;
    }

    public QName getName() {
        return ( (ObjectType) type ).getName();
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Node> getContents() {
        return contents;
    }
}
