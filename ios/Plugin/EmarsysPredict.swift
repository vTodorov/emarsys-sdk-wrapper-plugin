import Foundation
import Capacitor
import EmarsysSDK

public class EmarsysPredict {
    public func trackItemView(_ call: CAPPluginCall) {
        Emarsys.predict.trackItem(itemId: call.getString("itemId")!)
        call.resolve();
    }
    
    public func trackCategoryView(_ call: CAPPluginCall) {
        Emarsys.predict.trackCategory(categoryPath: call.getString("categoryPath")!)
        call.resolve();
    }
    
    public func trackSearchTerm(_ call: CAPPluginCall) {
        Emarsys.predict.trackSearch(searchTerm: call.getString("searchTerm")!)
        call.resolve();
    }
    
    public func trackTag(_ call: CAPPluginCall) {
        guard let eventName = call.getString("eventName") else {
              call.reject("eventName is required")
              return
          }

          let rawAttributes = call.getObject("attributes") ?? [:]
          var attributes: [String: String] = [:]

          for (key, value) in rawAttributes {
              attributes[key] = "\(value)"
          }

          Emarsys.predict.trackTag(tag:eventName, attributes: attributes)

          call.resolve()
    }
    
    public func trackCart(_ call: CAPPluginCall) {
        Emarsys.predict.trackCart(items: self.formatCardItems(items: call.getArray("items", JSObject.self) ?? []));
        call.resolve();
    }
    
    public func trackPurchase(_ call: CAPPluginCall) {
        Emarsys.predict.trackPurchase(orderId: call.getString("orderId")!, items: self.formatCardItems(items: call.getArray("cartItems", JSObject.self) ?? []));
        call.resolve();
    }
    
    public func recommendProducts(_ call: CAPPluginCall) {
        let emsLogic: EMSLogicProtocol;
        let logic = call.getObject("logic")!;
        let logicType = logic["type"] as! String;
        
        switch logicType {
        case "search":
            emsLogic = EMSLogic.search(searchTerm: logic["value"] as? String);
        case "cart":
            emsLogic = EMSLogic.cart(cartItems: self.formatCardItems(items: logic["value"]  as? [JSObject]));
        case "related":
            emsLogic = EMSLogic.related(itemId: logic["value"] as? String);
        case "category":
            emsLogic = EMSLogic.category(categoryPath: logic["value"] as? String);
        case "also_bought":
            emsLogic = EMSLogic.alsoBought(itemId: logic["value"] as? String);
        case "popular":
            emsLogic = EMSLogic.popular(categoryPath: logic["value"] as? String);
        case "personal":
            emsLogic = EMSLogic.personal(variants: logic["value"] as? [String]);
        case "home":
            emsLogic = EMSLogic.home(variants: logic["value"] as? [String]);
        default:
            call.reject("Unknwon logicType: " + logicType);
            return;
        }
        
        var emsFiter: [EMSRecommendationFilterProtocol]?;
        let filters = call.getArray("filter", JSObject.self) ?? [];
        if(filters.count > 0) {
            emsFiter = [EMSRecommendationFilterProtocol]();
            
            filters.forEach { filter in
                let filterType = filter["filterType"] as! String;
                let comparatorType = filter["comparatorType"] as! String;
                let field = filter["field"] as! String;
                if(filterType == "include") {
                    switch comparatorType {
                    case "isValue":
                        emsFiter!.append(EMSRecommendationFilter.include(withField: field, isValue: filter["value"] as! String));
                    case "inValues":
                        emsFiter!.append(EMSRecommendationFilter.include(withField: field, inValues: filter["value"] as! [String]));
                    case "hasValue":
                        emsFiter!.append(EMSRecommendationFilter.include(withField: field, hasValue: filter["value"] as! String));
                    case "overlapsValues":
                        emsFiter!.append(EMSRecommendationFilter.include(withField: field, overlapsValues: filter["value"] as! [String]));
                    default:
                        call.reject("Unkown comparatorType: " + comparatorType);
                        return;
                    }
                } else {
                    switch comparatorType {
                    case "isValue":
                        emsFiter!.append(EMSRecommendationFilter.excludeFilter(withField: field, isValue: filter["value"] as! String));
                    case "inValues":
                        emsFiter!.append(EMSRecommendationFilter.excludeFilter(withField: field, inValues: filter["value"] as! [String]));
                    case "hasValue":
                        emsFiter!.append(EMSRecommendationFilter.excludeFilter(withField: field, hasValue: filter["value"] as! String));
                    case "overlapsValues":
                        emsFiter!.append(EMSRecommendationFilter.excludeFilter(withField: field, overlapsValues: filter["value"] as! [String]));
                    default:
                        call.reject("Unkown comparatorType: " + comparatorType);
                        return;
                    }
                }
            }
        }
        
        let emsLimit = call.getInt("limit") as NSNumber?;
        
        let emsAvailabilityZone = call.getString("availabilityZone");
        
        Emarsys.predict.recommendProducts(logic: emsLogic, filters: emsFiter, limit: emsLimit, availabilityZone: emsAvailabilityZone) { products, error in
            guard error == nil else {
                call.reject(error!.localizedDescription)
                return
            }
            call.resolve(self.formatProduct(products: products));
        }
    }
    
    public func trackRecommendationClick(_ call: CAPPluginCall) {
        let product: EMSProduct = EMSProduct();
        // Having a look into the SDK itself only productId, feature and cohort are used here
        product.setValue(call.getString("productId")!, forKey: "productId");
        product.setValue(call.getString("feature")!, forKey: "feature");
        product.setValue(call.getString("cohort")!, forKey: "cohort");
        
        Emarsys.predict.trackRecommendationClick(product: product);
        call.resolve();
    }
    
    private func formatCardItems(items: [JSObject]?) -> [EMSCartItem] {
        var cardItems = [EMSCartItem]();
        items?.forEach { item in
            cardItems.append(EMSCartItem(itemId: item["item"] as? String, price: item["price"] as! Double, quantity: item["quantity"] as! Double))
        }
        return cardItems;
    }
    
    private func formatProduct(products: [EMSProductProtocol]?) -> PluginCallResultData {
        var jsProducts = [JSObject]();
        
        products?.forEach { product in
            var jsProduct = JSObject.init(dictionaryLiteral:
                                            ("productId", product.productId),
                                          ("title", product.title),
                                          ("linkUrl", product.linkUrl.absoluteString),
                                          ("customFields", JSTypes.coerceDictionaryToJSObject(product.customFields )!
                                          ),
                                          ("feature", product.feature),
                                          ("cohort", product.cohort)
            );
            
            if(product.imageUrl != nil) {
                jsProduct["imageUrl"] = product.imageUrl!.absoluteString;
            }
            if(product.zoomImageUrl != nil) {
                jsProduct["zoomImageUrl"] = product.zoomImageUrl!.absoluteString;
            }
            if(product.categoryPath != nil) {
                jsProduct["categoryPath"] = product.categoryPath!;
            }
            if(product.available != nil) {
                jsProduct["available"] = product.available!.intValue;
            }
            if(product.productDescription != nil) {
                jsProduct["productDescription"] = product.productDescription!;
            }
            if(product.price != nil) {
                jsProduct["price"] = product.price!.doubleValue;
            }
            if(product.msrp != nil) {
                jsProduct["msrp"] = product.price!.intValue;
            }
            if(product.album != nil) {
                jsProduct["album"] = product.album!;
            }
            if(product.actor != nil) {
                jsProduct["actor"] = product.actor!;
            }
            if(product.artist != nil) {
                jsProduct["artist"] = product.artist!;
            }
            if(product.author != nil) {
                jsProduct["author"] = product.author!;
            }
            if(product.brand != nil) {
                jsProduct["brand"] = product.brand!;
            }
            if(product.year != nil) {
                jsProduct["year"] = product.year!.intValue;
            }
            
            jsProducts.append(jsProduct);
        }
        
        return ["products": jsProducts];
    }
}
