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
package se.sics.caracaldb.datamodel.msg;

import se.sics.kompics.Event;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DMMessage {

    public static class Req extends Event {

        public final long id;

        public Req(long id) {
            this.id = id;
        }
    }

    public static class Resp extends Event {

        public final long id;
        public final ResponseCode respCode;

        public Resp(long id, ResponseCode respCode) {
            this.id = id;
            this.respCode = respCode;
        }
    }

    public static enum ResponseCode {

        SUCCESS, FAILURE;
    }
}
