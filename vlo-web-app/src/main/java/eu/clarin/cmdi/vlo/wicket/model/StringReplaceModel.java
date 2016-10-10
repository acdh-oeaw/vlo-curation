/*
 * Copyright (C) 2016 CLARIN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.clarin.cmdi.vlo.wicket.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 *
 * @author Twan Goosen <twan.goosen@mpi.nl>
 */
public class StringReplaceModel extends AbstractReadOnlyModel<String> {

    private final IModel<String> stringModel;
    private final IModel<Pattern> patternModel;
    private final IModel<String> replacementModal;

    public StringReplaceModel(IModel<String> stringModel, IModel<Pattern> patternModel, IModel<String> replacementModal) {
        this.stringModel = stringModel;
        this.patternModel = patternModel;
        this.replacementModal = replacementModal;
    }

    @Override
    public String getObject() {
        final String value = stringModel.getObject();
        if (value == null) {
            return null;
        }
        final Matcher matcher = patternModel.getObject().matcher(value);
        return matcher.replaceAll(replacementModal.getObject());
    }

    @Override
    public void detach() {
        stringModel.detach();
        patternModel.detach();
        replacementModal.detach();
    }

}
