/*
 * Copyright (C) 2014 CLARIN
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
package eu.clarin.cmdi.vlo.wicket.panels.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import eu.clarin.cmdi.vlo.pojo.FacetSelection;
import eu.clarin.cmdi.vlo.pojo.FacetSelectionValueQualifier;
import eu.clarin.cmdi.vlo.wicket.components.FieldValueLabel;

/**
 * A panel representing a single facet and its selected values, allowing for
 * deselection
 *
 * @author twagoo
 */
public abstract class SelectedFacetPanel extends GenericPanel<FacetSelection> {

    public SelectedFacetPanel(String id, String facetName, final IModel<FacetSelection> model) {
        super(id, model);

        // Add removers for all selected values for collapsed state
        add(createSelectionRemovers("facetValueRemover", facetName));
        // Add selected items to expanded state
        add(createSelectionRemovers("selectedItem", facetName));
    }

    private ListView<String> createSelectionRemovers(String id, String facetName) {
        // Model of the list of selected values in this facet
        final IModel<List<String>> valuesModel = new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return new ArrayList(SelectedFacetPanel.this.getModelObject().getValues());
            }

        };
        final IModel<String> fieldNameModel = new Model<>(facetName);

        // Repeating container of value + unselection links
        final ListView<String> listView = new ListView<String>(id, valuesModel) {

            /**
             * Populates an individual value selection remover
             *
             * @param item item to populate
             */
            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Label("negator", "- ") {

                    @Override
                    protected void onConfigure() {
                        setVisible(SelectedFacetPanel.this.getModelObject().getQualifier(item.getModelObject()) == FacetSelectionValueQualifier.NOT);
                    }

                });
                // A label showing the name of the facet
                item.add(new FieldValueLabel("facetValue", item.getModel(), fieldNameModel));
                // A link to remove the value selection from this facet
                item.add(new RemoveLink("unselectValue", item.getModel()));
            }

        };
        return listView;
    }

    /**
     * Callback triggered when values have been removed from this facet
     *
     * @param valuesRemoved removed values
     * @param target Ajax target allowing for a partial update. May be null
     * (fallback)!
     */
    protected abstract void onValuesUnselected(Collection<String> valuesRemoved, AjaxRequestTarget target);

    public class RemoveLink extends IndicatingAjaxFallbackLink {

        private final IModel<String> valueModel;

        public RemoveLink(String id, IModel<String> valueModel) {
            super(id);
            this.valueModel = valueModel;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            // Remove a single value
            // Call callback
            onValuesUnselected(Collections.singleton(valueModel.getObject()), target);
        }

    }
}
