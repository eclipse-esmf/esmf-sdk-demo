package com.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.jackson.AspectModelJacksonModule;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.FileSystemStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.esmf.metamodel.ModelElement;
import org.eclipse.esmf.staticmetamodel.constraint.StaticConstraintProperty;
import org.eclipse.esmf.staticmetamodel.propertychain.PropertyChain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.catenax.part_as_planned.MetaPartAsPlanned;
import io.catenax.part_as_planned.MetaPartTypeInformationEntity;
import io.catenax.part_as_planned.PartAsPlanned;
import io.catenax.part_as_planned.PartTypeInformationEntity;
import org.junit.jupiter.api.Test;

public class EsmfSdkTest {
   /**
    * Refer to the directory containing the Aspect Models to load.
    * The sldt-semantic-models are downloaded using the maven-scm-plugin, see "clone-sldt-semantic-models" in pom.xml.
    */
   private static final String BASE_DIR = "sldt-semantic-models";

   /**
    * The resolution strategy defines how to resolve Aspect Models; we'll use the file system directory here.
    * See <a
    * href="https://eclipse-esmf.github.io/esmf-developer-guide/tooling-guide/java-aspect-tooling.html#understanding-model-resolution">
    * Understanding Model Resolution</a> for more info on resolution strategies.
    */
   private static final ResolutionStrategy FILE_SYSTEM_STRATEGY = new FileSystemStrategy( Path.of( BASE_DIR ) );

   /**
    * Test that demonstrates how to load an Aspect Model by its URN.
    */
   @Test
   void loadAspectModelByUrn() {
      final AspectModelUrn aspectUrn = AspectModelUrn.fromUrn( "urn:samm:io.catenax.part_as_planned:2.0.0#PartAsPlanned" );

      assertThatCode( () -> {
         final AspectModel aspectModel = new AspectModelLoader( FILE_SYSTEM_STRATEGY ).load( aspectUrn );
         assertThat( aspectModel.aspects() ).isNotEmpty();
      } ).doesNotThrowAnyException();
   }

   /**
    * Test that demonstrates how to load an Aspect Model from a given File.
    */
   @Test
   void loadAspectModelByFile() {
      final File file = new File( System.getProperty( "user.dir" ) ).toPath()
            .resolve( BASE_DIR )
            .resolve( "io.catenax.part_as_planned" )
            .resolve( "2.0.0" )
            .resolve( "PartAsPlanned.ttl" )
            .toFile();

      assertThatCode( () -> {
         final AspectModel aspectModel = new AspectModelLoader( FILE_SYSTEM_STRATEGY ).load( file );
         assertThat( aspectModel.aspects() ).isNotEmpty();
      } ).doesNotThrowAnyException();
   }

   /**
    * Test that demonstrates how to load an Aspect Model from an InputStream.
    */
   @Test
   void loadAspectModelByInputStream() throws IOException {
      final File file = new File( System.getProperty( "user.dir" ) ).toPath()
            .resolve( BASE_DIR )
            .resolve( "io.catenax.part_as_planned" )
            .resolve( "2.0.0" )
            .resolve( "PartAsPlanned.ttl" )
            .toFile();

      try ( final InputStream input = new FileInputStream( file ) ) {
         assertThatCode( () -> {
            final AspectModel aspectModel = new AspectModelLoader( FILE_SYSTEM_STRATEGY ).load( input );
            assertThat( aspectModel.aspects() ).isNotEmpty();
         } ).doesNotThrowAnyException();
      }
   }

   /**
    * Test that demonstrates how to generate a JSON sample payload corresponding to an Aspect Model, then
    * using a <a href="https://github.com/FasterXML/jackson">Jackson</a> Object Mapper to parse this JSON into a
    * type-safe representation (i.e., an object of type PartAsPlanned). Note that the PartAsPlanned class is
    * generated during build time (see "generate-java-classes" in pom.xml).
    * See <a
    * href="https://eclipse-esmf.github.io/esmf-developer-guide/tooling-guide/java-aspect-tooling.html#generating-sample-json-payload">
    * Generating Sample JSON Payload</a> for more information.
    */
   @Test
   void generateSampleJsonAndParseJson() throws IOException {
      // Load Aspect Model
      final AspectModelUrn aspectUrn = AspectModelUrn.fromUrn( "urn:samm:io.catenax.part_as_planned:2.0.0#PartAsPlanned" );
      final AspectModel aspectModel = new AspectModelLoader( FILE_SYSTEM_STRATEGY ).load( aspectUrn );
      final Aspect aspect = aspectModel.aspects().stream().filter( a -> a.urn().equals( aspectUrn ) ).findFirst().orElseThrow();

      // Generate sample JSON payload
      final AspectModelJsonPayloadGenerator jsonGenerator = new AspectModelJsonPayloadGenerator( aspect );
      final String sampleJson = jsonGenerator.generateJson();

      System.out.println( "JSON for " + aspectUrn + ":" );
      System.out.println( sampleJson );
      System.out.println();

      // Create Jackson object mapper
      final ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule( new JavaTimeModule() );
      mapper.registerModule( new Jdk8Module() );
      mapper.registerModule( new AspectModelJacksonModule() );
      mapper.configure( JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true );
      mapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
      mapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );

      assertThatCode( () -> {
         // Parse JSON. We now have a type-safe representation of Aspect data.
         final PartAsPlanned partAsPlanned = mapper.readValue( sampleJson, PartAsPlanned.class );

         // Print the object
         final String objectAsString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString( partAsPlanned );
         System.out.println( "Java object with data:" );
         System.out.println( objectAsString );
      } ).doesNotThrowAnyException();
   }

   /**
    * Test that demonstrates generation of HTML documentation for an Aspect. See <a
    * href="https://eclipse-esmf.github.io/esmf-developer-guide/tooling-guide/java-aspect-tooling.html#generating-html-documentation">
    * Generating HTML Documentation</a> for for information.
    */
   @Test
   void generateDocumentationForAspectModel() throws IOException {
      // Load Aspect Model
      final AspectModelUrn aspectUrn = AspectModelUrn.fromUrn( "urn:samm:io.catenax.part_as_planned:2.0.0#PartAsPlanned" );
      final AspectModel aspectModel = new AspectModelLoader( FILE_SYSTEM_STRATEGY ).load( aspectUrn );
      final Aspect aspect = aspectModel.aspects().stream().filter( a -> a.urn().equals( aspectUrn ) ).findFirst().orElseThrow();

      // Generate HTML documentation
      final AspectModelDocumentationGenerator generator = new AspectModelDocumentationGenerator( aspect );
      final File output = new File( System.getProperty( "user.dir" ) ).toPath()
            .resolve( "target" )
            .resolve( "PartAsPlanned.html" )
            .toFile();
      try ( final FileOutputStream outputStream = new FileOutputStream( output ) ) {
         generator.generate( htmlFileName -> outputStream, Map.of(), Locale.ENGLISH );
      }
      assertThat( output ).exists();
      assertThat( output ).isFile();
      assertThat( output ).content().contains( "<html" );
   }

   /**
    * Demonstrate usage of a "static meta class" for an Aspect Model. See <a
    * href="https://eclipse-esmf.github.io/esmf-developer-guide/tooling-guide/java-aspect-tooling.html#generating-static-meta-classes">
    * Generating Static Meta Classes</a> for more  information.
    */
   @Test
   void useStaticMetaClass() {
      // Access the information from the model in a type-safe way
      new MetaPartAsPlanned().getProperties().forEach( property -> {
         System.out.println( "Property " + property.getName() + ": " );
         System.out.println( "  is complex type: " + property.isComplexType() );
         System.out.println( "  is optional as used here: " + property.isOptional() );
         System.out.println( "  containing type: " + property.getContainingType() );
         System.out.println( "  characteristic URN: " + property.getCharacteristic().map( ModelElement::urn )
               .map( AspectModelUrn::toString ).orElse( "" ) );
         if ( property instanceof final StaticConstraintProperty<?, ?, ?> constraintProperty ) {
            constraintProperty.getConstraints().forEach( constraint -> {
               System.out.println( "  constraint: " + constraint.toString() );
            } );
         }
         System.out.println();
      } );

      // But we can also statically refer to single elements:
      System.out.println( "catenaXId's Characteristic: " + MetaPartAsPlanned.CATENA_X_ID.getCharacteristic() );
   }

   /**
    * Demonstrates deserializing data corresponding to an Aspect Model into a type safe representation, then using the
    * (generated) "static meta class" for the Aspect Model to iterate over properties in a type-safe way and get the corresponding
    * piece of data.
    */
   @Test
   void deserializeAndUseData() throws JsonProcessingException {
      final String data = """
            {
              "partTypeInformation" : {
                "classification" : "product",
                "manufacturerPartId" : "123-0.740-3434-A",
                "nameAtManufacturer" : "Mirror left"
              },
              "partSitesInformationAsPlanned" : [ {
                "functionValidUntil" : "2024-07-19T08:19:46.729+02:00",
                "catenaXsiteId" : "BPNS1234567890ZZ",
                "function" : "production",
                "functionValidFrom" : "2024-07-19T08:19:46.729+02:00"
              } ],
              "catenaXId" : "580d3adf-1981-44a0-a214-13d6ceed9379"
            }
            """;

      final ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule( new JavaTimeModule() );
      mapper.registerModule( new Jdk8Module() );
      mapper.registerModule( new AspectModelJacksonModule() );
      mapper.configure( JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true );
      mapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
      mapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );

      // Parse the data into the type safe representation
      final PartAsPlanned partAsPlanned = mapper.readValue( data, PartAsPlanned.class );

      // Statically construct accessors for nested information
      final PropertyChain<PartAsPlanned, String> getManufacturerPartId =
            PropertyChain.from( MetaPartAsPlanned.PART_TYPE_INFORMATION )
                  .to( MetaPartTypeInformationEntity.MANUFACTURER_PART_ID );
      System.out.printf( "%s/%s: %s%n",
            MetaPartAsPlanned.PART_TYPE_INFORMATION.getName(),
            MetaPartTypeInformationEntity.MANUFACTURER_PART_ID.getName(),
            getManufacturerPartId.getValue( partAsPlanned ) );

      // Iterate values of properties in a type-safe and generic way: All you need to provide to this snippet
      // is the instance of the data class (partAsPlanned in this case).
      MetaPartAsPlanned.INSTANCE.getProperties().forEach( staticProperty -> {
         System.out.println( staticProperty.getName() + ": " + staticProperty.getValue( partAsPlanned ) );
      } );
      final PartTypeInformationEntity partTypeInformationEntity = MetaPartAsPlanned.PART_TYPE_INFORMATION.getValue( partAsPlanned );
      MetaPartTypeInformationEntity.INSTANCE.getProperties().forEach( staticProperty -> {
         System.out.println(
               "partTypeInformationEntity/" + staticProperty.getName() + ": " + staticProperty.getValue( partTypeInformationEntity ) );
      } );
   }
}
