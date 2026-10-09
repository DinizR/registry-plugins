package systems.porto.registry.entry.rest;

import systems.porto.adapter.entry.AbstractEntryAdapter;
import systems.porto.api.route.CrudHttpRoutes;
import systems.porto.api.route.EntryHttpRoutes;
import systems.porto.api.route.RouteRegistrar;
import systems.porto.context.Context;

public class ProductRestEntryAdapter extends AbstractEntryAdapter<Context> {

    @Override
    public String id() {
        return "product-rest";
    }

    @Override
    public void start() {
        RouteRegistrar registrar = EntryHttpRoutes.registrar(getContext());
        String apiBasePath = EntryHttpRoutes.apiBasePath(getConfig());
        CrudHttpRoutes.register(registrar, apiBasePath, "/products", "product-crud");
    }

    @Override
    public void stop() {
    }
}
