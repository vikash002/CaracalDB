/* 
 * This file is part of the CaracalDB distributed storage system.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.caracaldb.datatransfer;

import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.UUID;
import se.sics.caracaldb.Address;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class InitiateTransfer extends TransferMessage {
    
    public final ImmutableMap<String, Object> metadata;
    
    public InitiateTransfer(Address src, Address dst, UUID id, ImmutableMap<String, Object> metadata) {
        super(src, dst, id);
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("InitiateTransfer(");
        sb.append("id: ");
        sb.append(this.id);
        sb.append(", meta: \n");
        for (Entry<String, Object> e : metadata.entrySet()) {
            sb.append("   ");
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }
}
